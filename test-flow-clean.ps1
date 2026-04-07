$baseUrl = "http://localhost:8080"
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$productId = 1000 + ($runId % 1000000)
$email = "rahul+$runId@example.com"

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

    $probeUri = "$baseUrl/api/inventory/$productId/availability"
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)

    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-RestMethod -Method Get -Uri $probeUri | Out-Null
            Write-Host "Gateway routing is ready." -ForegroundColor Green
            return
        } catch {
            $statusCode = Get-HttpStatusCode $_
            if ($statusCode -eq 404) {
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
Wait-ForHealthyEndpoint -Name "User service" -Uri "http://localhost:8081/actuator/health"
Wait-ForHealthyEndpoint -Name "Order service" -Uri "http://localhost:8082/actuator/health"
Wait-ForHealthyEndpoint -Name "Inventory service" -Uri "http://localhost:8083/actuator/health"
Wait-ForGatewayRouting

Step "1. Run metadata"
Write-Host "runId: $runId"
Write-Host "productId: $productId"
Write-Host "email: $email"

Step "2. Create inventory"
$inventoryBody = @{
    productId = $productId
    productName = "iPhone 15 Run $runId"
    availableQuantity = 10
} | ConvertTo-Json
$inventory = Invoke-StepRequest { Invoke-RestMethod -Method Post -Uri "$baseUrl/api/inventory" -ContentType "application/json" -Body $inventoryBody }

Step "3. Check inventory before order"
$inventoryBefore = Invoke-StepRequest { Invoke-RestMethod -Method Get -Uri "$baseUrl/api/inventory/$productId" }
$availabilityBefore = Invoke-StepRequest { Invoke-RestMethod -Method Get -Uri "$baseUrl/api/inventory/$productId/availability" }

Step "4. Create user"
$userBody = @{
    name = "Rahul"
    email = $email
    address = "Bangalore"
} | ConvertTo-Json
$user = Invoke-StepRequest { Invoke-RestMethod -Method Post -Uri "$baseUrl/api/users" -ContentType "application/json" -Body $userBody }
$userId = if ($null -ne $user) { $user.id } else { $null }

Step "5. Place order"
$orderBody = @{
    userId = $userId
    productId = $productId
    quantity = 2
    unitPrice = 49999.00
} | ConvertTo-Json
if ($null -ne $userId) {
    $order = Invoke-StepRequest { Invoke-RestMethod -Method Post -Uri "$baseUrl/api/orders" -ContentType "application/json" -Body $orderBody }
    $orderId = if ($null -ne $order) { $order.orderId } else { $null }
} else {
    Write-Host "Skipping order creation because user id is unavailable." -ForegroundColor Yellow
    $orderId = $null
}

Step "6. Wait for Kafka processing"
Start-Sleep -Seconds 5

Step "7. Check order after async processing"
if ($null -ne $orderId) {
    $finalOrder = Invoke-StepRequest { Invoke-RestMethod -Method Get -Uri "$baseUrl/api/orders/$orderId" }
} else {
    Write-Host "Skipping order fetch because order id is unavailable." -ForegroundColor Yellow
    $finalOrder = $null
}

Step "8. Check inventory after order"
$inventoryAfter = Invoke-StepRequest { Invoke-RestMethod -Method Get -Uri "$baseUrl/api/inventory/$productId" }
$availabilityAfter = Invoke-StepRequest { Invoke-RestMethod -Method Get -Uri "$baseUrl/api/inventory/$productId/availability" }

Step "9. Summary"
Write-Host "UserId: $userId"
Write-Host "OrderId: $orderId"
Write-Host "ProductId: $productId"
if ($null -ne $finalOrder) {
    Write-Host "Final order status: $($finalOrder.status)" -ForegroundColor Green
}
if ($null -ne $inventoryAfter) {
    Write-Host "Inventory reserved quantity: $($inventoryAfter.reservedQuantity)" -ForegroundColor Green
}

Write-Host ""
Write-Host "Flow completed." -ForegroundColor Green
