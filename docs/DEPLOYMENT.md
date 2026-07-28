# Deployment Guide - Doctor-Patient-Portal

This guide provides instructions for containerizing and deploying the Doctor-Patient-Portal application to AWS EKS.

## Prerequisites

### Local Development
- Docker installed
- Java 8 JDK
- Maven 3.9+

### AWS EKS Deployment
- AWS CLI installed and configured
- `kubectl` installed
- Access to an AWS EKS Cluster
- IAM permissions to create ECR repositories and manage EKS resources

## Local Development Setup

1. **Build the application locally**:
   ```bash
   mvn clean package -DskipTests
   ```

2. **Run with Docker Compose**:
   ```bash
   docker-compose up -d
   ```
   The application will be available at `http://localhost:8080`.

## Build and Push to Registry

1. **Run the build script**:
   - Linux/macOS: `./scripts/build-push.sh`
   - Windows: `scripts\\build-push.bat`

2. **Follow the prompts**:
   - Select registry (AWS ECR or Docker Hub).
   - Provide registry credentials and image tag.
   - The script will build the image and push it to the selected registry.

## AWS EKS Deployment

1. **Run the deployment script**:
   - Linux/macOS: `./scripts/deploy-image.sh`
   - Windows: `scripts\\deploy-image.bat`

2. **Follow the prompts**:
   - Enter AWS region and EKS cluster name.
   - Provide the full Docker image URI.
   - Enter environment variables for database and Redis connections.

3. **Verification**:
   - The script will apply Kubernetes manifests and wait for the rollout.
   - Check the status using `kubectl get pods -n doctor-patient-portal`.

## Configuration Management

The application uses environment variables for configuration:
- `DB_URL`: JDBC connection string for MySQL.
- `DB_USER`: Database username.
- `DB_PASS`: Database password.
- `REDIS_HOST`: Redis host address.
- `REDIS_PORT`: Redis port.
- `REDIS_PASSWORD`: Redis password.

## Troubleshooting

- **Pod CrashLoopBackOff**: Check logs using `kubectl logs -l app=doctor-patient-portal -n doctor-patient-portal`.
- **Ingress Issues**: Ensure the AWS Load Balancer Controller is installed in your EKS cluster.
- **Database Connection**: Verify that the EKS cluster has network access to the MySQL and Redis instances.

## Security Considerations

- Use AWS Secrets Manager or Kubernetes Secrets for sensitive data instead of plain environment variables.
- The container runs as a non-root user (`appuser`) for improved security.
- NetworkPolicies should be implemented to restrict traffic between pods.
