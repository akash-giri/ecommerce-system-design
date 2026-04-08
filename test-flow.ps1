$baseUrl = "http://localhost:8080"
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$authUsername = "rahul-auth-$runId"
$authEmail = "rahul-auth+$runId@example.com"

function Step($message) {
    Write-Host ""
    Write-Host "==== $message ====" -ForegroundColor Cyan
}

function Show-Json($value) {
    $value | ConvertTo-Json -Depth 10
}

function Show-ErrorResponse($errorRecord) {
    if ($null -eq $errorRecord.Exception.Response) {
        Write-Host $errorRecord.Exception.Message -ForegroundColor Red
        return
    }

    $response = $errorRecord.Exception.Response
    $stream = $response.GetResponseStream()
    if ($null -eq $stream) {
        Write-Host $errorRecord.Exception.Message -ForegroundColor Red
        return
    }

    $reader = New-Object System.IO.StreamReader($stream)
    $body = $reader.ReadToEnd()
    $reader.Close()
    $stream.Close()

    if ([string]::IsNullOrWhiteSpace($body)) {
        Write-Host $errorRecord.Exception.Message -ForegroundColor Red
        return
    }

    Write-Host $body -ForegroundColor Red
}

function Invoke-StepRequest {
    param(
        [scriptblock]$Request
    )

    try {
        $result = & $Request
        if ($null -ne $result) {
            Show-Json $result
        }
        return $result
    } catch {
        Show-ErrorResponse $_
        return $null
    }
}

function Get-HttpStatusCode {
    param(
        $errorRecord
    )

    $response = $errorRecord.Exception.Response
    if ($null -eq $response) {
        return $null
    }

    if ($response -is [System.Net.HttpWebResponse]) {
        return [int]$response.StatusCode
    }

    try {
        return [int]$response.StatusCode
    } catch {
        return $null
    }
}

function Wait-ForHealthyEndpoint {
    param(
        [string]$Name,
        [string]$Uri,
        [int]$TimeoutSeconds = 180
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-RestMethod -Method Get -Uri $Uri
            if ($null -ne $response.status -and $response.status -eq "UP") {
                Write-Host "$Name is healthy." -ForegroundColor Green
                return
            }
        } catch {
        }

        Start-Sleep -Seconds 2
    }

    throw "$Name did not become healthy within $TimeoutSeconds seconds."
}

function Wait-ForGatewayRouting {
    param(
        [int]$TimeoutSeconds = 180
    )

    $probeUri = "$baseUrl/api/inventory/101/availability"
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)

    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-RestMethod -Method Get -Uri $probeUri | Out-Null
            Write-Host "Gateway routing is ready." -ForegroundColor Green
            return
        } catch {
            $statusCode = Get-HttpStatusCode $_
            if ($statusCode -in 401, 404) {
                Write-Host "Gateway routing is ready." -ForegroundColor Green
                return
            }
        }

        Start-Sleep -Seconds 2
    }

    throw "Gateway routing did not become ready within $TimeoutSeconds seconds."
}

Step "0. Wait for services to be ready"
Wait-ForHealthyEndpoint -Name "API gateway" -Uri "$baseUrl/actuator/health"
Wait-ForHealthyEndpoint -Name "Auth service" -Uri "http://localhost:8084/actuator/health"
Wait-ForHealthyEndpoint -Name "User service" -Uri "http://localhost:8081/actuator/health"
Wait-ForHealthyEndpoint -Name "Order service" -Uri "http://localhost:8082/actuator/health"
Wait-ForHealthyEndpoint -Name "Inventory service" -Uri "http://localhost:8083/actuator/health"
Wait-ForGatewayRouting

Step "1. Register and login auth user"
$registerBody = @{
    username = $authUsername
    email = $authEmail
    password = "Password@123"
} | ConvertTo-Json
Invoke-StepRequest { Invoke-RestMethod -Method Post -Uri "$baseUrl/auth/register" -ContentType "application/json" -Body $registerBody }
$loginBody = @{
    username = $authUsername
    password = "Password@123"
} | ConvertTo-Json
$auth = Invoke-StepRequest { Invoke-RestMethod -Method Post -Uri "$baseUrl/auth/login" -ContentType "application/json" -Body $loginBody }
$authHeaders = if ($null -ne $auth) { @{ Authorization = "Bearer $($auth.accessToken)" } } else { $null }

Step "2. Health checks"
Invoke-StepRequest { Invoke-RestMethod -Method Get -Uri "$baseUrl/actuator/health" }
Invoke-StepRequest { Invoke-RestMethod -Method Get -Uri "http://localhost:8084/actuator/health" }
Invoke-StepRequest { Invoke-RestMethod -Method Get -Uri "http://localhost:8081/actuator/health" }
Invoke-StepRequest { Invoke-RestMethod -Method Get -Uri "http://localhost:8082/actuator/health" }
Invoke-StepRequest { Invoke-RestMethod -Method Get -Uri "http://localhost:8083/actuator/health" }

Step "3. Create inventory"
$inventoryBody = @{
    productId = 101
    productName = "iPhone 15"
    availableQuantity = 10
} | ConvertTo-Json
Invoke-StepRequest { Invoke-RestMethod -Method Post -Uri "$baseUrl/api/inventory" -Headers $authHeaders -ContentType "application/json" -Body $inventoryBody }

Step "4. Check inventory"
Invoke-StepRequest { Invoke-RestMethod -Method Get -Uri "$baseUrl/api/inventory/101" -Headers $authHeaders }
Invoke-StepRequest { Invoke-RestMethod -Method Get -Uri "$baseUrl/api/inventory/101/availability" -Headers $authHeaders }

Step "5. Create user"
$userBody = @{
    name = "Rahul"
    email = "rahul+$runId@example.com"
    address = "Bangalore"
} | ConvertTo-Json
$user = Invoke-StepRequest { Invoke-RestMethod -Method Post -Uri "$baseUrl/api/users" -Headers $authHeaders -ContentType "application/json" -Body $userBody }
$userId = if ($null -ne $user) { $user.id } else { $null }

Step "6. Fetch user"
if ($null -ne $userId) {
    Invoke-StepRequest { Invoke-RestMethod -Method Get -Uri "$baseUrl/api/users/$userId" -Headers $authHeaders }
} else {
    Write-Host "Skipping fetch user because user creation did not return an id." -ForegroundColor Yellow
}

Step "7. Place order"
$orderBody = @{
    userId = $userId
    productId = 101
    quantity = 2
    unitPrice = 49999.00
} | ConvertTo-Json
if ($null -ne $userId) {
    $order = Invoke-StepRequest { Invoke-RestMethod -Method Post -Uri "$baseUrl/api/orders" -Headers $authHeaders -ContentType "application/json" -Body $orderBody }
    $orderId = if ($null -ne $order) { $order.orderId } else { $null }
} else {
    Write-Host "Skipping order creation because user id is unavailable." -ForegroundColor Yellow
    $orderId = $null
}

Step "8. Wait for Kafka consumers to update the order status"
Start-Sleep -Seconds 5

Step "9. Fetch order after async processing"
if ($null -ne $orderId) {
    Invoke-StepRequest { Invoke-RestMethod -Method Get -Uri "$baseUrl/api/orders/$orderId" -Headers $authHeaders }
} else {
    Write-Host "Skipping order fetch because order creation did not return an id." -ForegroundColor Yellow
}

Step "10. Fetch orders for user"
if ($null -ne $userId) {
    Invoke-StepRequest { Invoke-RestMethod -Method Get -Uri "$baseUrl/api/users/$userId/orders" -Headers $authHeaders }
    Invoke-StepRequest { Invoke-RestMethod -Method Get -Uri "$baseUrl/api/orders/user/$userId" -Headers $authHeaders }
} else {
    Write-Host "Skipping user order lookups because user id is unavailable." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Flow completed. Now verify DB, Kafka, and logs with the commands from the guide." -ForegroundColor Green
