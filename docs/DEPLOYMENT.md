# Doctor Patient Portal: deployment guide (AWS EKS)

Doctor Patient Portal (Cont-3 / Doctor-Cont) is a Java 8 servlet and JSP web application packaged as a WAR (`Doctor-Patient-Portal.war`). It runs on Apache Tomcat 9 on `amazoncorretto:8`, listens on port 8080, and keeps its HTTP sessions in Redis (Amazon ElastiCache) through Spring Session, so that several pods share sessions.

## 1. Prerequisites and system requirements

- Docker 20.10+ (build and local run), 2 GB free memory for the build.
- AWS CLI v2, configured with `aws configure` or SSO.
- kubectl matching the cluster version (within one minor version).
- eksctl (to create the cluster).
- IAM permissions: `ecr:*` on the repository (create, push), `eks:DescribeCluster`, `eks:ListClusters`, and, to set up the cluster, `eksctl`'s CloudFormation/EC2/IAM permissions.
- An Amazon ElastiCache for Redis cluster in the same VPC as the EKS nodes; its security group allows inbound TCP 6379 from the node/pod security group.
- A MySQL database. **Note:** the application code (`com.hms.db.DBConnection`) still connects to `jdbc:mysql://localhost:3306/hospital` with fixed credentials; this was not a blocker of the analysis and was left as it is. Before real use, the connection must be moved to environment variables (for example `DB_HOST`, `DB_USER`, `DB_PASSWORD`) and the database provided separately.
- The AWS Load Balancer Controller installed in the cluster (for the ALB ingress).

## 2. Local setup with Docker Compose

`docker-compose.yml` has one service, the application. Redis is not part of it: point it at an existing Redis.

```bash
export REDIS_HOST=host.docker.internal   # or the Redis endpoint
export REDIS_PORT=6379
export REDIS_SSL=false
export REDIS_PASSWORD=change-me           # the Redis AUTH token (empty value is not accepted by compose: set one)
docker compose up --build
```

Open http://localhost:8080/ and check http://localhost:8080/health (`{"status":"UP"}`). Tomcat logs are kept in the `tomcat-logs` volume.

## 3. Build and push the image

Run from the repository root.

Linux and macOS: `./scripts/build-push.sh`; Windows: `scripts\build-push.bat`.

The script asks for the registry (1. AWS ECR, 2. Docker Hub), then the registry's details (region and account ID, or Docker Hub user and password), then `IMAGE_TAG` (empty means `latest`). It builds with `docker build -f Dockerfile -t <registry>/<repo>:<tag> .`, logs in (for ECR: `aws ecr get-login-password ... | docker login --username AWS --password-stdin`), creates the ECR repository when it is missing, and pushes. Keep the printed image URI for the deployment.

## 4. AWS EKS: cluster setup and kubectl configuration

```bash
eksctl create cluster --name doctor-portal --region us-east-1 --nodegroup-name workers \
  --node-type t3.medium --nodes 2 --managed
aws eks update-kubeconfig --region us-east-1 --name doctor-portal
kubectl cluster-info
```

Install the AWS Load Balancer Controller (Helm chart `eks/aws-load-balancer-controller`) with an IRSA service account, as described in the AWS documentation; the ingress needs it. Associate the OIDC provider (`eksctl utils associate-iam-oidc-provider --cluster doctor-portal --approve`) if you use IRSA.

## 5. Kubernetes deployment walkthrough

`./scripts/deploy-image.sh` (or `scripts\deploy-image.bat`) asks for the AWS region, the EKS cluster name, the image URI, and the values `REDIS_HOST`, `REDIS_PORT`, `REDIS_SSL` and `REDIS_PASSWORD`. It then configures kubectl with `aws eks update-kubeconfig`, checks `kubectl cluster-info`, replaces the `{{...}}` placeholders in `kubernetes/deployment.yaml` (and restores the template at the end), applies the manifests in order, waits for `kubectl rollout status deployment/doctor-patient-portal -n doctor-patient-portal`, and shows the pods, services, ingress and the application's URL.

| Manifest | Content |
| --- | --- |
| `kubernetes/namespace.yaml` | Namespace `doctor-patient-portal` |
| (created by the script) | Secret `doctor-patient-portal-secrets` with the key `REDIS_PASSWORD`, created with `kubectl create secret generic ... --dry-run=client -o yaml \| kubectl apply -f -` after the namespace and before the deployment |
| `kubernetes/deployment.yaml` | 2 replicas, container `doctor-patient-portal` on port 8080, image `"{{IMAGE_URI}}"`, env `REDIS_HOST`, `REDIS_PORT`, `REDIS_SSL` as placeholders, `REDIS_PASSWORD` from the Secret, requests 250m CPU / 512Mi, limits 500m / 1Gi, liveness and readiness probes on `/health` |
| `kubernetes/service.yaml` | `doctor-patient-portal-service`, ClusterIP, port 80 to 8080 (no session affinity needed: sessions are in Redis) |
| `kubernetes/ingress.yaml` | `doctor-patient-portal-ingress`, AWS ALB (`kubernetes.io/ingress.class: alb`, `alb.ingress.kubernetes.io/scheme: internet-facing`), host `doctor-patient-portal.example.com`, path `/` to the service on port 80 |

Replace `doctor-patient-portal.example.com` with your DNS name and point a CNAME to the ALB address.

Rollback after a failed deployment: `kubectl rollout undo deployment/doctor-patient-portal -n doctor-patient-portal`.

## 6. Configuration management and environment variables

| Variable | Purpose | Where it comes from |
| --- | --- | --- |
| `REDIS_HOST` | Redis / ElastiCache endpoint for HttpSession storage | deployment.yaml placeholder, asked by deploy-image |
| `REDIS_PORT` | Redis port, default 6379 | deployment.yaml placeholder |
| `REDIS_SSL` | `true` when ElastiCache in-transit encryption is on, default `false` | deployment.yaml placeholder |
| `REDIS_PASSWORD` | Redis AUTH token, optional | Kubernetes Secret `doctor-patient-portal-secrets`, key `REDIS_PASSWORD` |
| `JAVA_OPTS` | JVM options | Dockerfile default, can be overridden |
| `TZ` | `UTC` | Dockerfile |

Sessions are stored by Spring Session (`com.hms.config.SessionConfig`, `SessionInitializer`) with a 30-minute inactivity timeout. Objects stored in the session (`User`, `Doctor`) are `Serializable`.

## 7. Security considerations and best practices

- Credentials (the Redis token) live in a Kubernetes Secret, never in the image, the manifests or the repository. Consider AWS Secrets Manager with the External Secrets Operator and IRSA for rotation.
- The container runs as the non-root user `app` (uid 1001).
- Use ElastiCache with in-transit encryption (`REDIS_SSL=true`) and an AUTH token; restrict the security group to the nodes.
- Serve the ingress over HTTPS (ACM certificate, `alb.ingress.kubernetes.io/certificate-arn` and `listen-ports` annotations).
- Known risks left as they are: the static admin login (`admin@gmail.com`/`admin`) in `AdminLoginServlet`, and the hard-coded MySQL connection in `DBConnection` (see the prerequisites). Fix both before production.
- Scan the image (ECR scanning) and keep the Tomcat and Corretto images updated.

## 8. Troubleshooting

- **Pod failures**: `kubectl get pods -n doctor-patient-portal`, `kubectl describe pod <pod> -n doctor-patient-portal`, `kubectl logs <pod> -n doctor-patient-portal`. `ImagePullBackOff`: check the image URI and that the node role can read ECR. `CrashLoopBackOff` or failing probes: read the Tomcat log; a wrong `REDIS_HOST` prevents the session filter from working. `OOMKilled`: raise the memory limit and `-Xmx`.
- **Service issues**: `kubectl get endpoints -n doctor-patient-portal` must list the pod IPs; the selector is `app: doctor-patient-portal`. Test with `kubectl port-forward svc/doctor-patient-portal-service 8080:80 -n doctor-patient-portal` and open `/health`.
- **Ingress problems**: `kubectl describe ingress doctor-patient-portal-ingress -n doctor-patient-portal`; no ADDRESS means the AWS Load Balancer Controller is missing or lacks IAM permissions (check its logs in `kube-system`). Unhealthy ALB targets: the health check path is `/health`.
- **EKS**: `aws eks update-kubeconfig` fails with an access error: check the IAM identity and the cluster's access entries / `aws-auth` ConfigMap. Pods cannot reach Redis: check the ElastiCache security group on 6379 and TLS settings.

## 9. Scaling and management

- Manual: `kubectl scale deployment/doctor-patient-portal --replicas=4 -n doctor-patient-portal`.
- HPA (needs metrics-server): `kubectl autoscale deployment doctor-patient-portal --cpu-percent=70 --min=2 --max=10 -n doctor-patient-portal`.
- Rolling update: set a new image with `kubectl set image deployment/doctor-patient-portal doctor-patient-portal=<new image URI> -n doctor-patient-portal` and follow with `kubectl rollout status`.
- Rollback: `kubectl rollout undo deployment/doctor-patient-portal -n doctor-patient-portal`; history: `kubectl rollout history deployment/doctor-patient-portal -n doctor-patient-portal`.
- Node capacity: `eksctl scale nodegroup --cluster doctor-portal --name workers --nodes 3`.

## 10. Java notes

- **Image**: three stages: `tomcat:9.0-jdk8-corretto` (only its Tomcat 9 distribution is copied), `maven:3.8.6-openjdk-8-slim` (builds the WAR with `mvn clean package -DskipTests`), and the runtime `amazoncorretto:8`. The WAR is deployed as `ROOT.war`, so the application is served at `/`. No health-check tool is installed and there is no `HEALTHCHECK`: Kubernetes probes `/health`.
- **JVM settings**: `JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`, plus UTF-8 and UTC. The explicit `-Xmx512m` fits the 1Gi limit with room for metaspace, threads and Netty buffers (Lettuce). If you raise the memory limit, raise `-Xmx` (or remove it and rely on `MaxRAMPercentage`) accordingly. `catalina.sh run` is started with `exec`, so Java receives SIGTERM and Tomcat shuts down gracefully.
- **Spring profiles**: the application is not a Spring Boot application, it only uses Spring Session for the session filter; there is no `SPRING_PROFILES_ACTIVE` and no profile files.
- **Health endpoint**: the application had none, so `GET /health` (`com.hms.health.HealthServlet`) answers `{"status":"UP"}` with HTTP 200 from start-up as long as Tomcat runs. It does not open a session and does not check the database or Redis. The liveness probe starts after 60 s and the readiness probe after 30 s (Tomcat plus Spring context start-up). There is no actuator.
