# Deployment Guide - Doctor Patient Portal

## Overview

Doctor Patient Portal is a Java 8 web application built with Maven, packaged as a WAR, and deployed on Apache Tomcat 9. This guide covers building, pushing, and deploying the application to AWS EKS (Elastic Kubernetes Service).

## Prerequisites

### System Requirements

- **Java**: JDK 8 (for local builds)
- **Maven**: 3.6+ (for local builds)
- **Docker**: 20.10+ (for container builds)
- **AWS CLI**: 2.x (for ECR and EKS)
- **kubectl**: 1.24+ (for Kubernetes deployment)
- **eksctl**: 0.110+ (for EKS cluster creation, optional)

### AWS Permissions

The deploying IAM user/role must have permissions for:

- ECR: `ecr:GetAuthorizationToken`, `ecr:BatchCheckLayerAvailability`, `ecr:CompleteLayerUpload`, `ecr:UploadLayerPart`, `ecr:PutLayerPart`, `ecr:InitiateLayerUpload`, `ecr:BatchGetImage`, `ecr:CreateRepository`, `ecr:DescribeRepositories`
- EKS: `eks:DescribeCluster`, `eks:ListClusters`
- STS: `sts:GetCallerIdentity`

## Local Setup with Docker Compose

### Environment Variables

The application reads the following environment variables:

| Variable       | Description                  | Default     |
|----------------|------------------------------|-------------|
| `DB_HOST`      | MySQL database host          | `localhost` |
| `DB_PORT`      | MySQL database port          | `3306`      |
| `DB_NAME`      | MySQL database name          | `hospital`  |
| `DB_USER`      | MySQL database user          | `root`      |
| `DB_PASSWORD`  | MySQL database password      | *(empty)*   |
| `REDIS_HOST`   | Redis host for sessions      | `localhost` |
| `REDIS_PORT`   | Redis port for sessions      | `6379`      |
| `REDIS_PASSWORD` | Redis auth password       | *(empty)*   |

### Running Locally

```bash
# Set environment variables
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=hospital
export DB_USER=root
export DB_PASSWORD=your_password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=your_redis_password

# Build and run with Docker Compose
docker-compose up
```

The application will be available at `http://localhost:8080`.
The health check endpoint is at `http://localhost:8080/health`.

## Building and Pushing to AWS ECR

### Using the build script (Linux/macOS)

```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

The script will prompt for:
1. AWS region
2. ECR repository name
3. Image tag

### Using the build script (Windows)

```cmd
scripts\build-push.bat
```

The script will prompt for the same values.

### Manual Build

```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Build
docker build -f Dockerfile -t <account-id>.dkr.ecr.us-east-1.amazonaws.com/doctor-patient-portal:latest .

# Push
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/doctor-patient-portal:latest
```

## Deploying to AWS EKS

### EKS Cluster Setup

If you don't have an EKS cluster:

```bash
eksctl create cluster \
  --name doctor-patient-portal-cluster \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-max 4
```

Or use the AWS Management Console to create an EKS cluster.

### Deploying the Application

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

The script will prompt for:
1. AWS region
2. EKS cluster name
3. Full image URI (from the build-push step)
4. Environment variable values (DB_HOST, DB_PORT, DB_NAME, DB_USER, REDIS_HOST, REDIS_PORT)
5. Credential values (DB_PASSWORD, REDIS_PASSWORD) — entered without echo

The script will:
1. Configure kubectl for the EKS cluster
2. Apply the namespace
3. Create a Kubernetes Secret with credentials
4. Substitute placeholders in the deployment manifest
5. Apply the deployment, service, and ingress
6. Wait for the rollout to complete
7. Display the deployed resources

### Manual Deployment

```bash
# Configure kubectl
aws eks update-kubeconfig --region us-east-1 --name <cluster-name>

# Apply namespace
kubectl apply -f kubernetes/namespace.yaml

# Create Secret with credentials
kubectl create secret generic doctor-patient-portal-secrets \
  -n doctor-patient-portal \
  --from-literal=DB_PASSWORD=<db-password> \
  --from-literal=REDIS_PASSWORD=<redis-password> \
  --dry-run=client -o yaml | kubectl apply -f -

# Apply deployment (after substituting placeholders)
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml

# Wait for rollout
kubectl rollout status deployment/doctor-patient-portal -n doctor-patient-portal
```

## Kubernetes Manifests

### Namespace (`kubernetes/namespace.yaml`)

Creates a dedicated namespace `doctor-patient-portal` for the application.

### Secret (`kubernetes/secret.yaml`)

A template Secret containing `DB_PASSWORD` and `REDIS_PASSWORD` keys. The deploy script creates the actual Secret with real values via `kubectl create secret`.

### Deployment (`kubernetes/deployment.yaml`)

- **Replicas**: 2 (for high availability)
- **Container**: Runs the WAR on Tomcat 9, port 8080
- **Environment Variables**: DB connection details and Redis connection details
- **Secrets**: DB_PASSWORD and REDIS_PASSWORD are sourced from the Kubernetes Secret
- **Resources**:
  - Requests: 250m CPU, 512Mi memory
  - Limits: 500m CPU, 1Gi memory
- **Liveness Probe**: HTTP GET to `/health` on port 8080 (30s initial delay, 30s interval)
- **Readiness Probe**: HTTP GET to `/health` on port 8080 (15s initial delay, 10s interval)

### Service (`kubernetes/service.yaml`)

- **Type**: ClusterIP
- **Port**: 80 → 8080 (container port)
- Exposes the application within the cluster

### Ingress (`kubernetes/ingress.yaml`)

- **Ingress Class**: ALB (AWS Load Balancer Controller)
- **Scheme**: Internet-facing
- **Host**: `doctor-patient-portal.example.com`
- Routes traffic from port 80 to the service

## Configuration Management

### Externalized Configuration

All configuration is externalized to environment variables:

- **Database**: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- **Redis**: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`

Non-credential values are set as plain environment variables in the deployment.
Credential values (`DB_PASSWORD`, `REDIS_PASSWORD`) are stored in a Kubernetes Secret and referenced via `valueFrom.secretKeyRef`.

### Session Management

HTTP sessions are externalized to Redis using Spring Session. The `DelegatingFilterProxy` named `sessionRepositoryFilter` intercepts all requests and transparently stores session data in Redis. This means:

- Sessions persist across pod restarts
- Sessions are shared across multiple pod replicas
- No sticky sessions (session affinity) needed on the Service

## Security Considerations

1. **Credentials**: All credentials (DB password, Redis password) are stored in Kubernetes Secrets, never in source code or container images.
2. **Non-root container**: The container runs as a non-root user (`tomcat`).
3. **Minimal base image**: The runtime stage uses `amazoncorretto:8` with no package manager installed.
4. **Read-only filesystem**: Consider adding `readOnlyRootFilesystem: true` to the container security context in production.
5. **Network policies**: Consider adding Kubernetes Network Policies to restrict pod communication.
6. **Redis TLS**: Enable Redis in-transit encryption by setting `spring.redis.ssl=true` when connecting to ElastiCache with encryption in transit.

## Troubleshooting

### Pod Failures

```bash
# Check pod status
kubectl get pods -n doctor-patient-portal

# Check pod logs
kubectl logs <pod-name> -n doctor-patient-portal

# Describe pod for events
kubectl describe pod <pod-name> -n doctor-patient-portal
```

Common issues:
- **ImagePullBackOff**: Verify the image URI and ECR permissions
- **CrashLoopBackOff**: Check logs for startup errors, verify environment variables
- **ErrImagePull**: Verify the image tag exists in ECR

### Service Issues

```bash
# Check service
kubectl get svc -n doctor-patient-portal

# Check endpoints
kubectl get endpoints -n doctor-patient-portal
```

Common issues:
- **No endpoints**: Verify the deployment is running and the readiness probe passes

### Ingress Problems

```bash
# Check ingress
kubectl get ingress -n doctor-patient-portal

# Check ingress events
kubectl describe ingress doctor-patient-portal-ingress -n doctor-patient-portal
```

Common issues:
- **No address**: Wait for the ALB to be provisioned (may take 2-5 minutes)
- **404 errors**: Verify the host header matches the ingress rule

## Scaling and Management

### Horizontal Pod Autoscaler

```bash
kubectl autoscale deployment doctor-patient-portal \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n doctor-patient-portal
```

### Rolling Updates

```bash
# Update the image
kubectl set image deployment/doctor-patient-portal \
  doctor-patient-portal=<new-image-uri> \
  -n doctor-patient-portal

# Check rollout status
kubectl rollout status deployment/doctor-patient-portal -n doctor-patient-portal

# Rollback if needed
kubectl rollout undo deployment/doctor-patient-portal -n doctor-patient-portal
```

### Manual Rollback

```bash
kubectl rollout undo deployment/doctor-patient-portal -n doctor-patient-portal
```

## Java Notes

### JVM Settings

The container uses the following JVM settings (via `JAVA_OPTS`):

| Flag | Value | Purpose |
|------|-------|---------|
| `-Xmx512m` | Maximum heap size | Caps heap at 512MB |
| `-Xms256m` | Initial heap size | Pre-allocates 256MB |
| `-XX:+UseContainerSupport` | Container awareness | Enables container-aware memory limits |
| `-XX:MaxRAMPercentage=75.0` | Max RAM percentage | Uses 75% of container memory limit for heap |

These settings are tuned for the container's resource limits (512Mi request, 1Gi limit). The heap is capped at 512MB, and with `MaxRAMPercentage=75.0`, the JVM will use 75% of the container's memory limit (768MB) for the heap if the container has more memory available.

### Container Memory Tuning

The deployment requests 512Mi and limits 1Gi of memory. The JVM settings ensure:
- Initial heap: 256MB (fits within the 512Mi request)
- Maximum heap: 512MB (fits within the 1Gi limit)
- Container-aware: JVM respects container memory limits via `UseContainerSupport`

If you need to adjust memory, update both the deployment resource limits and the `JAVA_OPTS` environment variable in the Dockerfile.

### Spring Session Configuration

HTTP sessions are managed by Spring Session with Redis as the backing store:

- **Redis connection**: Configured via `REDIS_HOST`, `REDIS_PORT`, and `REDIS_PASSWORD` environment variables
- **Session timeout**: 1800 seconds (30 minutes), configured via `@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)`
- **Filter**: `DelegatingFilterProxy` named `sessionRepositoryFilter` intercepts all requests
- **Serialization**: Spring Session handles serialization of session attributes to Redis

### Health Check Endpoint

The application exposes a health check endpoint at `/health` that returns `{"status":"UP"}`. This endpoint is used by:

- **Liveness probe**: Determines if the container should be restarted
- **Readiness probe**: Determines if the container is ready to receive traffic

### Logging

The application uses Tomcat's default logging configuration. Spring Boot's logging starter (included via `spring-boot-starter-data-redis`) provides SLF4J with Logback as the logging implementation. Logs are written to stdout/stderr for container log aggregation.
