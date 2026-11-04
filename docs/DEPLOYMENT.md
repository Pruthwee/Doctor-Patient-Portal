# Deployment Guide - Doctor Patient Portal

## Overview

This guide covers building, pushing, and deploying the Doctor Patient Portal application to **AWS EKS** (Elastic Kubernetes Service). The application is a Java 8 servlet-based web application packaged as a WAR file, served by Apache Tomcat 8.5 on Amazon Corretto 8.

## Prerequisites

### System Requirements
- Java 8 JDK (for local development and builds)
- Maven 3.6+
- Docker 20.10+
- AWS CLI 2.x
- kubectl 1.24+
- eksctl 0.150+

### AWS Permissions Required
The IAM user/role deploying the application needs the following permissions:
- `ecr:GetAuthorizationToken`, `ecr:BatchCheckLayerAvailability`, `ecr:CompleteLayerUpload`, `ecr:UploadLayerPart`, `ecr:InitiateLayerUpload`, `ecr:PutImage`, `ecr:BatchGetImage`
- `eks:DescribeCluster`, `eks:ListClusters`
- `eks:AccessKubernetesCluster` (via kubectl)

### Kubernetes RBAC
The EKS cluster must have the AWS Load Balancer Controller installed for ingress support. See [AWS documentation](https://docs.aws.amazon.com/eks/latest/userguide/aws-load-balancer-controller.html) for setup instructions.

## Local Setup with Docker Compose

### Environment Variables

The application reads the following environment variables:

| Variable | Description | Example |
|---|---|---|
| `DB_HOST` | MySQL database host | `localhost` |
| `DB_PORT` | MySQL database port | `3306` |
| `DB_NAME` | MySQL database name | `hospital` |
| `DB_USER` | MySQL database username | `root` |
| `DB_PASSWORD` | MySQL database password | `secret` |
| `REDIS_HOST` | Redis host for Spring Session | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis auth password | `secret` |

### Running Locally

1. Ensure a MySQL database and Redis instance are running and accessible.
2. Run with Docker Compose:

```bash
docker-compose up --build
```

3. The application will be available at `http://localhost:8080`.
4. Health check endpoint: `http://localhost:8080/health`

## Building and Pushing the Docker Image

### Using build-push.sh (Linux/macOS)

```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

The script will:
1. Ask whether to push to AWS ECR or Docker Hub.
2. Prompt for the registry details (AWS region, registry URL, repository name, tag).
3. Log in to the chosen registry.
4. Build the Docker image with `docker build -f Dockerfile -t <registry>/<repo>:<tag> .`.
5. Push the image.

For AWS ECR, the script will also create the repository if it does not exist.

### Using build-push.bat (Windows)

```cmd
scripts\build-push.bat
```

The script follows the same flow as the shell version, using Windows commands and PowerShell for string sanitization.

### Image Details

- **Image name**: `doctor-patient-portal`
- **Base image (runtime)**: `amazoncorretto:8`
- **Build image**: `maven:3.9.4-eclipse-temurin-8`
- **Port**: 8080 (Tomcat default)
- **Health endpoint**: `/health`

## Deploying to AWS EKS

### EKS Cluster Setup

1. **Create an EKS cluster** (if you do not have one):

```bash
eksctl create cluster --name <cluster-name> --region <aws-region> --nodegroup-name <node-group-name> --node-type t3.medium --nodes 2 --nodes-max 4
```

2. **Associate the OIDC provider** (required for IRSA if using AWS Secrets Manager):

```bash
aws eks associate-oidc-provider --cluster-name <cluster-name> --region <aws-region> --approve
```

3. **Install the AWS Load Balancer Controller** (required for ingress):

```bash
helm repo add eks https://aws.github.io/eks-helm-charts
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=<cluster-name> \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller \
  --set region=<aws-region> \
  --set vpcId=<vpc-id>
```

### kubectl Configuration

```bash
aws eks update-kubeconfig --region <aws-region> --name <cluster-name>
kubectl cluster-info
```

### Deploying the Application

1. **Build and push the Docker image** using `scripts/build-push.sh` (see above).

2. **Deploy using deploy-image.sh** (Linux/macOS):

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

The script will:
1. Ask for the AWS region and EKS cluster name.
2. Configure kubectl for the target cluster.
3. Ask for the full image URI (e.g., `123456789012.dkr.ecr.us-east-1.amazonaws.com/doctor-patient-portal:latest`).
4. Ask for environment variables (DB_HOST, DB_PORT, DB_NAME, REDIS_HOST, REDIS_PORT) or skip them.
5. Ask for credentials (DB_USER, DB_PASSWORD, REDIS_PASSWORD) without echoing.
6. Create a Kubernetes Secret with the credentials.
7. Replace placeholders in `kubernetes/deployment.yaml`.
8. Apply the namespace, deployment, service, and ingress manifests.
9. Wait for the deployment rollout to complete.
10. Display the deployment status and application URL.

3. **Deploy using deploy-image.bat** (Windows):

```cmd
scripts\deploy-image.bat
```

The script follows the same flow as the shell version.

### Kubernetes Manifests

The `kubernetes/` directory contains:

| File | Description |
|---|---|
| `namespace.yaml` | Creates the `doctor-patient-portal` namespace |
| `deployment.yaml` | Deployment with 2 replicas, resource limits, health probes, and env vars |
| `service.yaml` | ClusterIP service mapping port 80 to container port 8080 |
| `ingress.yaml` | Ingress with AWS ALB annotations for internet-facing access |

### Environment Variables in Kubernetes

Non-credential environment variables are set as plain values in the deployment. Credential values (DB_USER, DB_PASSWORD, REDIS_PASSWORD) are stored in a Kubernetes Secret named `doctor-patient-portal-secrets` and referenced via `secretKeyRef`.

### Health Checks

The application exposes a `/health` endpoint that returns `{"status":"UP"}`. The Kubernetes deployment configures:
- **Liveness probe**: HTTP GET on `/health` at port 8080, with a 30-second initial delay.
- **Readiness probe**: HTTP GET on `/health` at port 8080, with a 15-second initial delay.

### Rollback Instructions

If the deployment fails:

```bash
kubectl rollout undo deployment/doctor-patient-portal -n doctor-patient-portal
```

To check rollout history:

```bash
kubectl rollout history deployment/doctor-patient-portal -n doctor-patient-portal
```

## Configuration Management

### Environment Variables

All configuration is externalized via environment variables. No credentials are stored in the application code or configuration files.

| Variable | Source | Description |
|---|---|---|
| `DB_HOST` | Kubernetes env / Docker env | MySQL database host |
| `DB_PORT` | Kubernetes env / Docker env | MySQL database port |
| `DB_NAME` | Kubernetes env / Docker env | MySQL database name |
| `DB_USER` | Kubernetes Secret | MySQL username |
| `DB_PASSWORD` | Kubernetes Secret | MySQL password |
| `REDIS_HOST` | Kubernetes env / Docker env | Redis host for Spring Session |
| `REDIS_PORT` | Kubernetes env / Docker env | Redis port |
| `REDIS_PASSWORD` | Kubernetes Secret | Redis auth password |

### Secrets Management

Credentials are stored in a Kubernetes Secret (`doctor-patient-portal-secrets`) and injected into the pod via `secretKeyRef`. The deploy script creates this secret from user-provided values.

### Session Externalization

The application uses Spring Session with Redis to externalize HTTP session storage. This ensures:
- Sessions persist across pod restarts.
- Sessions are shared across multiple pod replicas.
- No sticky sessions are required on the load balancer.

Redis connection is configured via `REDIS_HOST`, `REDIS_PORT`, and `REDIS_PASSWORD` environment variables. Provision an Amazon ElastiCache for Redis cluster in the same VPC as your EKS nodes.

## Security Considerations and Best Practices

1. **No hard-coded credentials**: All database and Redis credentials are read from environment variables or Kubernetes Secrets.
2. **Non-root container**: The Docker image runs as a non-root user (`tomcat`, UID 1000).
3. **Minimal attack surface**: The Tomcat manager and host-manager applications are removed from the Docker image.
4. **Session security**: Sessions are stored in Redis (ElastiCache), not in container memory. This prevents session loss on pod restart and enables horizontal scaling.
5. **Health checks**: The `/health` endpoint allows Kubernetes to detect and restart unhealthy pods.
6. **Resource limits**: CPU and memory requests/limits are set to prevent resource exhaustion.
7. **TLS for Redis**: When connecting to ElastiCache with in-transit encryption, set `spring.redis.ssl=true` (note: this requires additional Spring Session configuration).

## Java Notes

### JVM Settings

The Docker image sets the following JVM options via `JAVA_OPTS`:

```
-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

- `-Xmx512m`: Maximum heap size of 512 MB.
- `-Xms256m`: Initial heap size of 256 MB.
- `-XX:+UseContainerSupport`: Enables container-aware JVM (Java 8u191+).
- `-XX:MaxRAMPercentage=75.0`: Uses up to 75% of the container's memory limit for the heap.

These settings are tuned for the Kubernetes resource limits:
- **Requests**: 250m CPU, 512Mi memory
- **Limits**: 500m CPU, 1Gi memory

### Spring Profiles

This application does not use Spring Boot. The `SPRING_PROFILES_ACTIVE` environment variable is not applicable. Session configuration is handled by Spring Session's `@EnableRedisHttpSession` annotation on the `com.hms.config.SessionConfig` class.

### Health Endpoint

The application exposes a `/health` endpoint via `com.hms.health.HealthCheckServlet`. This endpoint returns `{"status":"UP"}` with HTTP 200. Kubernetes uses this endpoint for liveness and readiness probes.

### Session Externalization

The application uses Spring Session with Redis (ElastiCache) for session storage. The `SessionConfig` class at `com.hms.config.SessionConfig` configures:
- `@EnableRedisHttpSession`: Enables Spring Session with Redis as the backend.
- `LettuceConnectionFactory`: Configured from `REDIS_HOST`, `REDIS_PORT`, and `REDIS_PASSWORD` environment variables.

In `web.xml`, the `DelegatingFilterProxy` named `springSessionRepositoryFilter` intercepts all requests and routes session operations to Redis. The `ContextLoaderListener` loads the Spring application context from `SessionConfig`.

### Application Type

This is a traditional Java servlet web application (WAR packaging) deployed to Apache Tomcat 8.5. It is not a Spring Boot application. The WAR is built with Maven and deployed as `ROOT.war` in the Tomcat container.

## Troubleshooting

### Pod Failures

Check pod logs:
```bash
kubectl logs <pod-name> -n doctor-patient-portal
```

Describe pod for events:
```bash
kubectl describe pod <pod-name> -n doctor-patient-portal
```

### Service Issues

Check service endpoints:
```bash
kubectl get endpoints doctor-patient-portal-service -n doctor-patient-portal
```

### Ingress Problems

Check ingress status:
```bash
kubectl get ingress doctor-patient-portal-ingress -n doctor-patient-portal
```

Ensure the AWS Load Balancer Controller is installed and the ingress class annotation is correct.

### Session Issues

Ensure the Redis (ElastiCache) cluster is accessible from the EKS nodes. Check:
- Security group rules allow traffic on port 6379 from EKS nodes.
- Redis auth token matches the `REDIS_PASSWORD` secret.
- If using in-transit encryption, configure SSL in the Redis connection.

## Scaling and Management

### Horizontal Pod Autoscaler (HPA)

To create an HPA:

```bash
kubectl autoscale deployment doctor-patient-portal --cpu-percent=70 --min=2 --max=10 -n doctor-patient-portal
```

### Rolling Updates

Update the image:
```bash
kubectl set image deployment/doctor-patient-portal doctor-patient-portal=<new-image-uri> -n doctor-patient-portal
```

Monitor rollout:
```bash
kubectl rollout status deployment/doctor-patient-portal -n doctor-patient-portal
```

### Manual Rollback

```bash
kubectl rollout undo deployment/doctor-patient-portal -n doctor-patient-portal
```
