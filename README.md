# Ecommerce System Design Demo

This project is a Dockerized Spring Boot microservices demo with:

- `api-gateway`
- `auth-service`
- `discovery-server` (Eureka)
- `config-server`
- `user-service`
- `order-service`
- `inventory-service`
- MySQL for users and orders
- MongoDB for inventory
- Kafka for async order and stock events

The project now supports a working end-to-end flow through:

- JWT login through `auth-service`
- gateway-side token validation
- direct service APIs
- API gateway routes
- Kafka async processing
- MySQL and Mongo persistence
- Eureka service discovery

## Project Folder

Run all commands from:

```powershell
C:\Users\Admin\OneDrive\Documents\New project
```

Example:

```powershell
cd "C:\Users\Admin\OneDrive\Documents\New project"
```

## Available Test Scripts

### `test-flow.ps1`

Basic end-to-end flow through the API gateway.

Use this when you want a simple smoke test.

### `test-flow-clean.ps1`

Repeatable end-to-end flow through the API gateway using:

- a unique product id on each run
- a unique email on each run
- a unique auth user on each run
- automatic readiness checks before requests start

Use this for normal testing. This is the recommended script.

## First-Time Startup

If this is your first run, or you changed code/configuration, use:

```powershell
docker compose build --no-cache
docker compose up -d
```

Then run the clean test flow:

```powershell
.\test-flow-clean.ps1
```

## Normal Startup

If images are already built and you did not change code, use:

```powershell
docker compose up -d
```

Then run:

```powershell
.\test-flow-clean.ps1
```

Do not use `docker compose run` for this project.  
Use `docker compose up -d` so the full stack starts together.

## Recommended Daily Workflow

```powershell
cd "C:\Users\Admin\OneDrive\Documents\New project"
docker compose up -d
.\test-flow-clean.ps1
docker compose down
```

## When to Rebuild

Run a rebuild when you change:

- Java code
- `application.yml`
- `docker-compose.yml`
- Dockerfiles
- service configuration

Use:

```powershell
docker compose build --no-cache
docker compose up -d
.\test-flow-clean.ps1
```

## What `test-flow-clean.ps1` Does

The clean script:

1. waits for all services to become healthy
2. waits for gateway routing to become ready
3. waits for the gateway-to-auth-service route to be ready in Eureka/load-balancing
4. registers and logs in an auth user to get a JWT
5. creates a unique inventory item
6. creates a unique business user
7. waits for the gateway-to-order-service route to be ready in Eureka/load-balancing
8. places an order
9. waits for Kafka async processing
10. verifies the order reaches `STOCK_CONFIRMED`
11. verifies inventory after reservation

Expected successful outcome:

- order is created
- inventory reserves stock
- Kafka publishes and consumes events
- final order status becomes `STOCK_CONFIRMED`

## Manual Health Checks

Use these if you want to verify service health manually:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8084/actuator/health
Invoke-RestMethod http://localhost:8081/actuator/health
Invoke-RestMethod http://localhost:8082/actuator/health
Invoke-RestMethod http://localhost:8083/actuator/health
```

All should return:

```json
{
  "status": "UP"
}
```

## Manual Gateway Checks

First register and log in:

```powershell
$register = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/auth/register" -ContentType "application/json" -Body '{"username":"admin-demo-user","email":"demo-user@example.com","password":"Password@123"}'
$login = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/auth/login" -ContentType "application/json" -Body '{"username":"admin-demo-user","password":"Password@123"}'
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
```

Then call protected routes:

```powershell
Invoke-RestMethod http://localhost:8080/api/users/1 -Headers $headers
Invoke-RestMethod http://localhost:8080/api/orders/1 -Headers $headers
Invoke-RestMethod http://localhost:8080/api/inventory/101 -Headers $headers
```

Note: direct service ports such as `8081`, `8082`, and `8083` are still open for local development and debugging. Gateway auth is the enforced path in this sprint.

## Gateway Rate Limiting

The gateway enforces Redis-backed rate limiting on `/api/**` routes using Spring Cloud Gateway's `RequestRateLimiter` + Redis.

If you exceed the limit, you will see `429 Too Many Requests` and response headers such as:

- `X-RateLimit-Remaining`
- `X-RateLimit-Replenish-Rate`
- `X-RateLimit-Burst-Capacity`

## Forwarded Identity Headers (Gateway -> Services)

For this security sprint, the API gateway is the authentication boundary:

- you authenticate via `auth-service` (`/auth/register`, `/auth/login`)
- the gateway validates the JWT on every `/api/**` call
- the gateway forwards identity to services using headers:
  - `X-Authenticated-User`
  - `X-Authenticated-UserId`
  - `X-Authenticated-Role`
- the gateway also adds `X-Gateway-Secret` (a shared dev-only secret) so services can reject spoofed headers

Services now enforce these forwarded headers for all `/api/**` endpoints (actuator + swagger are excluded).

RBAC demo rule: only `ADMIN` role can `POST /api/inventory` (inventory upsert).

If you call a service directly on ports `8081/8082/8083` for debugging, it will return `401` unless you include the forwarded headers and `X-Gateway-Secret`. For normal testing, always call via the gateway (`8080`) and use `test-flow-clean.ps1`.

## Gateway Circuit Breaker + Fallbacks

The gateway uses a circuit breaker per route to prevent cascading failures when a backend is slow or down.

If a backend call fails (connection error / timeout), the gateway returns a stable JSON fallback with `503` instead of hanging.

How to test quickly:

```powershell
docker compose stop inventory-service
Invoke-RestMethod http://localhost:8080/api/inventory/101/availability -Headers @{ Authorization = "Bearer <JWT>" }
docker compose start inventory-service
```

You should receive a `503` response from `/fallback/inventory-service`.

## Idempotent Order Creation

The order creation flow now supports the `Idempotency-Key` request header.

Why this matters:

- distributed systems often retry requests after timeouts or temporary `503` errors
- retrying a `POST /api/orders` call without idempotency can create duplicate orders
- with `Idempotency-Key`, the same retried request returns the existing order instead of creating another one

The clean test script now sends an idempotency key for order creation automatically.

## Observability (Prometheus + Grafana)

This repo exposes Prometheus metrics on each service at:

`/actuator/prometheus`

Docker Compose includes:

- Prometheus on `http://localhost:9090`
- Grafana on `http://localhost:3000` (login `admin` / `admin`)

Start the stack and observability:

```powershell
docker compose up -d
```

### 1) Verify Prometheus Is Scraping Metrics

Prometheus works by "scraping" (polling) each service's `/actuator/prometheus` endpoint on a fixed interval.

Verify Prometheus targets are up:

```powershell
Invoke-RestMethod http://localhost:9090/api/v1/targets
```

You should see all jobs in `health=up`:

- `api-gateway`
- `auth-service`
- `user-service`
- `order-service`
- `inventory-service`

### 2) Explore Metrics in Prometheus

Open Prometheus UI:

- `http://localhost:9090`

Try queries like:

- `http_server_requests_seconds_count`
- `rate(http_server_requests_seconds_count[1m])`
- `jvm_memory_used_bytes`
- `resilience4j_circuitbreaker_state`

### 3) View Metrics in Grafana

Grafana is a dashboard tool. It queries Prometheus and renders charts.

Open Grafana:

- `http://localhost:3000` (login `admin` / `admin`)

Grafana is pre-provisioned with:

- a Prometheus datasource (uid `prometheus`)
- a dashboard folder: `Spring Boot`

#### Golden Signals Dashboard

Open:

- `Dashboards` -> `Spring Boot` -> `Golden Signals (Spring Boot Microservices)`

This dashboard shows the "golden signals":

- traffic (RPS)
- errors (5xx rate)
- latency (p95)
- saturation (JVM heap used)

These charts update as soon as you run traffic (for example `.\test-flow-clean.ps1`).

Dashboard source file:

- [golden-signals.json](/C:/Users/Admin/OneDrive/Documents/New%20project/observability/grafana/dashboards/golden-signals.json)

## Distributed Tracing (Jaeger)

Docker Compose includes Jaeger on:

- UI: `http://localhost:16686`

Traces are exported via OTLP HTTP to:

- `http://jaeger:4318/v1/traces` (inside Docker network)

Run an end-to-end request flow:

```powershell
.\test-flow-clean.ps1
```

Then open Jaeger UI and search for services like `api-gateway`, `order-service`, `inventory-service`.

### Trace-to-Log Correlation

Most HTTP request logs include `traceId` and `spanId` (Micrometer tracing MDC) so you can copy a trace id from Jaeger and search in:

```powershell
docker compose logs api-gateway --tail 200
docker compose logs order-service --tail 200
```

Note: the gateway also logs an `X-Correlation-Id` per request (see `CorrelationIdFilter`). This is often the easiest ID to follow in gateway logs.

## Alerting (Prometheus Rules + Alertmanager)

Prometheus evaluates alert rules on a schedule. When a rule is true for a configured duration (`for:`),
Prometheus fires an alert and sends it to Alertmanager.

Docker Compose includes:

- Alertmanager UI: `http://localhost:9093`

Prometheus is configured to send alerts to Alertmanager, and alert rules live here:

- [alerts.yml](/C:/Users/Admin/OneDrive/Documents/New%20project/observability/prometheus/rules/alerts.yml)

### How to See Alerts Firing (Quick Demo)

1. Start everything:

```powershell
docker compose up -d
```

2. Stop one service (this should trigger `ServiceDown` after ~30s):

```powershell
docker compose stop user-service
```

3. Open:

- Prometheus alerts: `http://localhost:9090/alerts`
- Alertmanager alerts: `http://localhost:9093/#/alerts`

4. Start the service again and watch the alert resolve:

```powershell
docker compose start user-service
```

## Centralized Logging (Loki + Promtail + Grafana)

This project now includes a simple but production-style log pipeline:

- Spring Boot services write logs to shared files under `./logs`
- Promtail tails those log files
- Loki stores and indexes the log streams
- Grafana lets you search the logs

Docker Compose includes:

- Loki API: `http://localhost:3100`
- Promtail: internal scraper for the log files

### How It Works

Each app container gets:

- `LOGGING_FILE_NAME=/logs/<service>.log`
- a shared volume mount from host `./logs` to container `/logs`

So, for example:

- `api-gateway` writes to `./logs/api-gateway.log`
- `order-service` writes to `./logs/order-service.log`

Promtail watches `./logs/*.log`, extracts labels from each line, and sends them to Loki.

Important labels extracted from logs:

- `service`
- `level`
- `traceId`
- `spanId`

### How to View Logs

1. Start the stack:

```powershell
docker compose up -d
```

2. Generate some traffic:

```powershell
.\test-flow-clean.ps1
```

3. Open Grafana:

- `http://localhost:3000`

4. Go to `Explore`

5. Choose datasource `Loki`

6. Try queries like:

```text
{job="spring-boot-logs"}
{service="api-gateway"}
{service="order-service", level="INFO"}
{traceId!=""}
```

### Why This Matters

This completes the observability triangle for the project:

- Prometheus = metrics
- Jaeger = traces
- Loki = logs

So now you can explain a real production debugging flow:

1. Grafana shows a latency spike
2. Jaeger shows which service/span was slow
3. Loki shows the exact logs for that service or `traceId`

## Database Verification

### User DB

```powershell
docker exec -it mysql-user mysql -uroot -proot -D userdb -e "select * from users;"
```

### Order DB

```powershell
docker exec -it mysql-order mysql -uroot -proot -D orderdb -e "select id,user_id,product_id,quantity,total_amount,status,created_at,updated_at from orders;"
```

### Inventory DB

```powershell
docker exec -it mongo-inventory mongosh inventorydb --eval "db.inventory_items.find().pretty()"
docker exec -it mongo-inventory mongosh inventorydb --eval "db.processed_events.find().pretty()"
```

## Kafka / Service Log Verification

Use these to inspect async event processing:

```powershell
docker compose logs inventory-service --tail 100
docker compose logs order-service --tail 100
docker compose logs api-gateway --tail 100
```

Healthy async flow should show messages similar to:

- `Received order-created event ...`
- `Published stock-updated event ...`
- `Received stock update event ...`
- `Order ... updated to STOCK_CONFIRMED`

## Stop the System

To stop containers:

```powershell
docker compose down
```

## Full Reset

To stop containers and remove volumes:

```powershell
docker compose down -v
```

Use `down -v` only when you want to clear MySQL and Mongo data and start fresh.

## Troubleshooting

### Script returns `503 Server Unavailable`

Usually happens during cold start (first requests after containers start) because:

- gateway timeouts are intentionally short (fail-fast)
- the first order request may take longer due to warm-up (JIT, DB pool, Kafka producer init)
- Eureka registration/load-balancer discovery can lag behind container health for a few seconds

Use:

```powershell
.\test-flow-clean.ps1
```

It waits for routing readiness, waits for the gateway-to-auth-service route to become usable, and retries order creation on transient `502/503/504`.
It also retries inventory and user creation on transient `502/503/504`, which makes startup demos much more reliable after container restarts.

### Protected route returns `401 Unauthorized`

This means the gateway is running, but the request did not include a valid bearer token.

Use:

```powershell
.\test-flow-clean.ps1
```

or log in manually through `/auth/login` and send the returned JWT in the `Authorization` header.

### Order stays in `CREATED`

Check Kafka logs:

```powershell
docker compose logs inventory-service --tail 100
docker compose logs order-service --tail 100
```

If Kafka is healthy, the order should move to `STOCK_CONFIRMED`.

### Health check fails

Start the stack:

```powershell
docker compose up -d
```

If needed, rebuild:

```powershell
docker compose build --no-cache
docker compose up -d
```

## Recommended Command Summary

### Start

```powershell
cd "C:\Users\Admin\OneDrive\Documents\New project"
docker compose up -d
```

### Test

```powershell
.\test-flow-clean.ps1
```

### Stop

```powershell
docker compose down
```

## Kubernetes + Helm (Minikube)

This project also supports a local Kubernetes demo using the Helm chart under `./helm/ecommerce`.

### 1. Start Minikube

```powershell
minikube start --driver=docker
kubectl create namespace ecommerce --dry-run=client -o yaml | kubectl apply -f -
```

### 2. Build Images (Local)

```powershell
docker compose build api-gateway auth-service discovery-server config-server user-service order-service inventory-service
```

### 3. Load Images Into Minikube

Minikube frequently cannot pull public images reliably from inside the cluster (TLS timeouts), so we pre-load images.

```powershell
minikube image load newproject-api-gateway:latest
minikube image load newproject-auth-service:latest
minikube image load newproject-discovery-server:latest
minikube image load newproject-config-server:latest
minikube image load newproject-user-service:latest
minikube image load newproject-order-service:latest
minikube image load newproject-inventory-service:latest

# Observability images
docker pull prom/prometheus:v2.54.1
docker pull grafana/grafana:11.1.0
docker pull jaegertracing/all-in-one:1.57.0
minikube image load prom/prometheus:v2.54.1
minikube image load grafana/grafana:11.1.0
minikube image load jaegertracing/all-in-one:1.57.0
```

### 4. Deploy With Helm

```powershell
helm upgrade --install ecommerce .\helm\ecommerce -n ecommerce
```

Wait until pods are running:

```powershell
kubectl get pods -n ecommerce
```

Note: on minikube, Spring apps may take a few minutes to warm up on the first boot.

### 5. Open UIs (Port-Forward)

Run each of these in a separate PowerShell window:

```powershell
kubectl port-forward -n ecommerce svc/api-gateway 8080:8080
kubectl port-forward -n ecommerce svc/prometheus 9090:9090
kubectl port-forward -n ecommerce svc/grafana 3000:3000
kubectl port-forward -n ecommerce svc/jaeger 16686:16686
```

Open:

- Gateway: `http://localhost:8080/actuator/health`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (admin/admin)
- Jaeger: `http://localhost:16686`

### 6. Generate Traffic

Once the gateway is port-forwarded, run the existing flow script against `http://localhost:8080`:

```powershell
.\test-flow-clean.ps1
```

Then verify:

- Prometheus Targets: `http://localhost:9090/targets`
- Grafana Explore (Prometheus datasource)
- Jaeger Traces (service names like `api-gateway`, `order-service`, etc.)
