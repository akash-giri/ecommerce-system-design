# Ecommerce System Design Demo

This project is a Dockerized Spring Boot microservices demo with:

- `api-gateway`
- `discovery-server` (Eureka)
- `config-server`
- `user-service`
- `order-service`
- `inventory-service`
- MySQL for users and orders
- MongoDB for inventory
- Kafka for async order and stock events

The project now supports a working end-to-end flow through:

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
3. creates a unique inventory item
4. creates a unique user
5. places an order
6. waits for Kafka async processing
7. verifies the order reaches `STOCK_CONFIRMED`
8. verifies inventory after reservation

Expected successful outcome:

- order is created
- inventory reserves stock
- Kafka publishes and consumes events
- final order status becomes `STOCK_CONFIRMED`

## Manual Health Checks

Use these if you want to verify service health manually:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
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

Examples:

```powershell
Invoke-RestMethod http://localhost:8080/api/users/1
Invoke-RestMethod http://localhost:8080/api/orders/1
Invoke-RestMethod http://localhost:8080/api/inventory/101
```

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

Usually happens when the gateway is up but routing is not fully ready yet.

Use:

```powershell
.\test-flow-clean.ps1
```

It already waits for routing readiness before sending requests.

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
