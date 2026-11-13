# Deployment Guide — Doctor-Patient-Portal

## Prerequisites

### System Requirements

- **Java**: JDK 8 (the project targets Java 1.8)
- **Maven**: 3.6+ (for building the WAR)
- **Docker**: 20.10+ (for building and running the container)
- **AWS CLI**: 2.x (for ECR login and EKS cluster management)
- **kubectl**: 1.24+ (for Kubernetes deployment)
- **eksctl**: 0.149+ (optional, for EKS cluster creation)

### AWS Permissions

The IAM user/role deploying the application needs:

- `ecr:GetAuthorizationToken`, `ecr:BatchCheckLayerAvailability`, `ecr:CompleteLayerUpload`, `ecr:UploadLayerPart`, `ecr:InitiateLayerUpload`, `ecr:PutImage`, `ecr:BatchGetImage` — for pushing images to ECR
- `eks:DescribeCluster` — for updating kubeconfig
- `ec2:CreateSecurityGroup`, `ec2:AuthorizeSecurityGroupIngress`, etc. — if creating EKS clusters
- Kubernetes RBAC permissions to create namespaces, deployments, services, ingresses, and secrets

### External Services

The application requires the following external services (provisioned separately):

1. **MySQL Database** — the application connects via `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
2. **Amazon ElastiCache for Redis** — for Spring Session externalized session storage, via `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`

---

## Local Setup with Docker Compose

### 1. Build and run locally

```bash
# Set environment variables for local testing
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=hospital
export DB_USER=root
export DB_PASSWORD=your_password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=your_redis_password

# Build and start
docker-compose up --build
```

The application will be available at `http://localhost:8080`.
The health check endpoint is at `http://localhost:8080/health`.

### 2. Stop locally

```bash
docker-compose down
```

---

## Building and Pushing the Container Image

### Using the build script (Linux/macOS)

```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

The script will:
1. Ask you to choose a target platform (AWS ECR or Docker Hub)
2. Ask for the registry details and image tag
3. Build the Docker image
4. Log in to the registry
5. Create the ECR repository if it doesn't exist (for ECR)
6. Push the image

### Using the build script (Windows)

```cmd
scripts\build-push.bat
```

### Manual Docker build

```bash
# Build
docker build -f Dockerfile -t doctor-patient-portal:latest .

# Tag for ECR
docker tag doctor-patient-portal:latest 123456789012.dkr.ecr.us-east-1.amazonaws.com/doctor-patient-portal:latest

# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com

# Push
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/doctor-patient-portal:latest
```

---

## AWS EKS Deployment

### 1. Prerequisites

#### Install AWS CLI

```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
```

#### Install kubectl

```bash
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```

#### Install eksctl (optional, for cluster creation)

```bash
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
```

#### Configure AWS credentials

```bash
aws configure
```

### 2. EKS Cluster Setup

If you don't have an EKS cluster:

```bash
# Create EKS cluster
eksctl create cluster \
  --name doctor-patient-portal-cluster \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-max 4

# Associate IAM OIDC provider (required for IRSA)
eksctl utils associate-iam-oidc-provider \
  --region us-east-1 \
  --cluster doctor-patient-portal-cluster \
  --approve
```

### 3. Provision External Services

#### MySQL Database

Provision a MySQL-compatible database (Amazon RDS recommended) in the same VPC as your EKS cluster. Note the host, port, database name, username, and password.

#### Amazon ElastiCache for Redis

Provision an ElastiCache for Redis cluster in the same VPC:

```bash
# Example using AWS CLI
aws elasticache create-cache-cluster \
  --cache-cluster-id doctor-patient-portal-redis \
  --cache-node-type cache.t3.micro \
  --engine redis \
  --num-cache-nodes 1
```

Note the Redis endpoint and auth token.

### 4. Deploy the Application

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

The script will:
1. Ask for the AWS region and EKS cluster name
2. Ask for the full image URI (from the build-push step)
3. Ask for environment variable values (DB_HOST, DB_PORT, etc.)
4. Ask for credentials (DB_PASSWORD, REDIS_PASSWORD) without echoing
5. Configure kubectl for the EKS cluster
6. Create a Kubernetes Secret with the credentials
7. Replace placeholders in deployment.yaml and apply all manifests
8. Wait for the deployment rollout to complete
9. Display the deployed resources and application URL

### 5. Manual Deployment (without scripts)

```bash
# Configure kubectl
aws eks update-kubeconfig --region us-east-1 --name doctor-patient-portal-cluster

# Create namespace
kubectl apply -f kubernetes/namespace.yaml

# Create secret with credentials
kubectl create secret generic doctor-patient-portal-secrets \
  -n doctor-patient-portal \
  --from-literal=DB_PASSWORD=your_db_password \
  --from-literal=REDIS_PASSWORD=your_redis_password \
  --dry-run=client -o yaml | kubectl apply -f -

# Replace placeholders and apply deployment
sed 's|{{IMAGE_URI}}|123456789012.dkr.ecr.us-east-1.amazonaws.com/doctor-patient-portal:latest|g' kubernetes/deployment.yaml | \
sed 's|{{DB_HOST}}|your-db-host|g' | \
sed 's|{{DB_PORT}}|3306|g' | \
sed 's|{{DB_NAME}}|hospital|g' | \
sed 's|{{DB_USER}}|root|g' | \
sed 's|{{REDIS_HOST}}|your-redis-host|g' | \
sed 's|{{REDIS_PORT}}|6379|g' | \
kubectl apply -f -

# Apply service and ingress
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml

# Wait for rollout
kubectl rollout status deployment/doctor-patient-portal -n doctor-patient-portal
```

---

## Kubernetes Manifests

### Namespace (`kubernetes/namespace.yaml`)

Creates a dedicated namespace `doctor-patient-portal` for the application.

### Deployment (`kubernetes/deployment.yaml`)

- **Replicas**: 2 (for high availability)
- **Image**: `{{IMAGE_URI}}` (replaced during deployment)
- **Container Port**: 8080 (Tomcat HTTP)
- **Environment Variables**:
  - `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` — MySQL connection (non-secret, from deployment)
  - `DB_PASSWORD` — from Kubernetes Secret `doctor-patient-portal-secrets`
  - `REDIS_HOST`, `REDIS_PORT` — Redis connection (non-secret, from deployment)
  - `REDIS_PASSWORD` — from Kubernetes Secret `doctor-patient-portal-secrets`
- **Resources**:
  - Requests: CPU 250m, Memory 512Mi
  - Limits: CPU 500m, Memory 1Gi
- **Liveness Probe**: HTTP GET `/health` on port 8080, 30s initial delay, 15s interval
- **Readiness Probe**: HTTP GET `/health` on port 8080, 15s initial delay, 10s interval

### Service (`kubernetes/service.yaml`)

- **Type**: ClusterIP
- **Port**: 80 → targetPort 8080
- **Selector**: `app: doctor-patient-portal`

### Ingress (`kubernetes/ingress.yaml`)

- **Ingress Class**: ALB (AWS Load Balancer Controller)
- **Scheme**: Internet-facing
- **Host**: `doctor-patient-portal.example.com`
- **Path**: `/` → service `doctor-patient-portal-service` port 80

---

## Configuration Management

### Environment Variables

| Variable | Description | Default | Required |
|---|---|---|---|
| `DB_HOST` | MySQL database host | `localhost` | Yes |
| `DB_PORT` | MySQL database port | `3306` | Yes |
| `DB_NAME` | MySQL database name | `hospital` | Yes |
| `DB_USER` | MySQL database user | `root` | Yes |
| `DB_PASSWORD` | MySQL database password | — | Yes |
| `REDIS_HOST` | Redis host (ElastiCache) | `localhost` | Yes |
| `REDIS_PORT` | Redis port | `6379` | Yes |
| `REDIS_PASSWORD` | Redis auth token | — | Yes |

### Kubernetes Secrets

Database and Redis credentials are stored in a Kubernetes Secret named `doctor-patient-portal-secrets` in the `doctor-patient-portal` namespace. The secret is created during deployment with `kubectl create secret generic`.

### Container Environment Variables

The Dockerfile sets the following environment variables for JVM tuning:

- `JAVA_OPTS`: `-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`
- `TZ`: `UTC`
- `JAVA_TOOL_OPTIONS`: `-Dfile.encoding=UTF-8 -Duser.timezone=UTC`

---

## Security Considerations

1. **Credentials**: Database and Redis passwords are never stored in the image. They are passed via environment variables or Kubernetes Secrets.
2. **Non-root user**: The container runs as a non-root user (`tomcat`) for reduced attack surface.
3. **Base image**: The runtime uses `eclipse-temurin:8-jdk-alpine`, a minimal base image.
4. **Session externalization**: HTTP sessions are stored in Redis (ElastiCache), not in the container's memory. This prevents session loss on pod restarts and enables horizontal scaling.
5. **TLS**: For production, enable TLS on the ALB ingress and on Redis (ElastiCache in-transit encryption).
6. **Network policies**: Consider applying Kubernetes NetworkPolicies to restrict traffic between pods.

---

## Troubleshooting

### Pod failures

```bash
# Check pod status
kubectl get pods -n doctor-patient-portal

# Check pod logs
kubectl logs <pod-name> -n doctor-patient-portal

# Describe pod for events
kubectl describe pod <pod-name> -n doctor-patient-portal
```

### Service issues

```bash
# Check service
kubectl get svc -n doctor-patient-portal

# Describe service
kubectl describe svc doctor-patient-portal-service -n doctor-patient-portal
```

### Ingress problems

```bash
# Check ingress
kubectl get ingress -n doctor-patient-portal

# Describe ingress
kubectl describe ingress doctor-patient-portal-ingress -n doctor-patient-portal

# Check ALB logs (if enabled)
```

### Session issues

If sessions are not persisting:

1. Verify Redis is accessible from the EKS pods
2. Check the `REDIS_HOST`, `REDIS_PORT`, and `REDIS_PASSWORD` environment variables
3. Verify the Kubernetes Secret was created: `kubectl get secret doctor-patient-portal-secrets -n doctor-patient-portal`
4. Check Redis connection in pod logs

### Database connection issues

1. Verify the MySQL database is accessible from the EKS cluster
2. Check the `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, and `DB_PASSWORD` environment variables
3. Verify the Kubernetes Secret for `DB_PASSWORD` was created

### Rollback

```bash
# Rollback to previous deployment
kubectl rollout undo deployment/doctor-patient-portal -n doctor-patient-portal

# Check rollout status
kubectl rollout status deployment/doctor-patient-portal -n doctor-patient-portal
```

---

## Scaling and Management

### Horizontal Pod Autoscaler (HPA)

```bash
kubectl autoscale deployment doctor-patient-portal \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n doctor-patient-portal
```

### Rolling Updates

To deploy a new image:

```bash
# Update the image
kubectl set image deployment/doctor-patient-portal \
  doctor-patient-portal=123456789012.dkr.ecr.us-east-1.amazonaws.com/doctor-patient-portal:new-tag \
  -n doctor-patient-portal

# Watch the rollout
kubectl rollout status deployment/doctor-patient-portal -n doctor-patient-portal
```

### Manual Scaling

```bash
# Scale up
kubectl scale deployment doctor-patient-portal --replicas=4 -n doctor-patient-portal

# Scale down
kubectl scale deployment doctor-patient-portal --replicas=2 -n doctor-patient-portal
```

---

## Java Notes

### JVM Settings

The container uses the following JVM settings (via `JAVA_OPTS`):

- `-Xmx512m` — Maximum heap size of 512 MB
- `-Xms256m` — Initial heap size of 256 MB
- `-XX:+UseContainerSupport` — Enables container-aware memory management (Docker-aware)
- `-XX:MaxRAMPercentage=75.0` — Maximum heap is 75% of the container's memory limit

These settings are tuned for the container's resource limits (512Mi request, 1Gi limit). The JVM will use up to 75% of the 1Gi limit (~768MB) for the heap, with the remaining memory available for off-heap allocations, thread stacks, and native libraries.

### Spring Profiles

This application does not use Spring Boot profiles. The session configuration is managed by `SessionConfig.java`, which reads Redis connection properties from environment variables.

### Health Check Endpoint

The application exposes a health check endpoint at `/health` (HTTP GET). It returns a JSON response:

```json
{"status":"UP"}
```

This endpoint is used by Kubernetes liveness and readiness probes. It is available immediately on application startup and remains available for the lifetime of the process.

### Session Externalization

HTTP sessions are externalized to Redis using Spring Session:

- `SessionConfig.java` configures `@EnableRedisHttpSession` with a `RedisConnectionFactory` bean
- Redis connection properties are read from environment variables (`REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`)
- The `DelegatingFilterProxy` in `web.xml` intercepts all `HttpSession` calls and routes them to Redis
- No changes to the application's servlet code are needed — the `HttpSession` API is used as before, but backed by Redis
- This enables horizontal scaling (multiple pod replicas) and session persistence across pod restarts
