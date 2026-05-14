# Kubernetes + Helm (Next Milestone)

This repo includes a Helm chart for the Spring microservices under:

`helm/ecommerce`

It deploys:

- discovery-server (Eureka)
- config-server
- api-gateway
- auth-service
- user-service
- order-service
- inventory-service

## What This Chart Assumes

This chart deploys only the Spring microservices. It assumes dependencies already exist in the cluster:

- `kafka` (bootstrap `kafka:9092`)
- `redis` (`redis:6379`)
- MySQL services: `mysql-auth`, `mysql-user`, `mysql-order`
- MongoDB service: `mongo-inventory`
- Jaeger OTLP HTTP: `jaeger:4318`

In the next step we can Helm-deploy those dependencies too (Bitnami charts), but keeping the first Helm milestone focused makes it easier to learn.

## Local Cluster Options

### Option A: kind

Create a cluster:

```powershell
kind create cluster --name ecommerce
```

### Option B: minikube

Start minikube:

```powershell
minikube start
```

## Install the Chart

## Install Helm (Windows)

If you do not have Helm installed, install it using one of these:

### Option A: Scoop

```powershell
scoop install helm
```

### Option B: Chocolatey

```powershell
choco install kubernetes-helm
```

Verify:

```powershell
helm version
```

From repo root:

```powershell
helm upgrade --install ecommerce ./helm/ecommerce -n ecommerce --create-namespace
```

Validate rendering without installing:

```powershell
helm template ecommerce ./helm/ecommerce -n ecommerce
```

Watch pods:

```powershell
kubectl get pods -n ecommerce -w
```

## Access the Gateway

For local debugging, port-forward the gateway:

```powershell
kubectl port-forward -n ecommerce svc/api-gateway 8080:8080
```

Then use the normal scripts against `http://localhost:8080`.

## Notes on Images

The chart references images like:

- `newproject-api-gateway:latest`
- `newproject-order-service:latest`

If your cluster cannot pull these images from a registry, you have two common paths:

1. Push images to a registry (Docker Hub, GHCR) and set `image.repositoryPrefix` accordingly.
2. Load images into the local cluster:
   - kind: `kind load docker-image newproject-api-gateway:latest --name ecommerce`
   - minikube: `minikube image load newproject-api-gateway:latest`

For kind, you will need to load each microservice image you built locally:

- `newproject-discovery-server:latest`
- `newproject-config-server:latest`
- `newproject-api-gateway:latest`
- `newproject-auth-service:latest`
- `newproject-user-service:latest`
- `newproject-order-service:latest`
- `newproject-inventory-service:latest`
