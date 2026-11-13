@echo off
setlocal enabledelayedexpansion

echo === Deploy doctor-patient-portal to AWS EKS ===

set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "
set /p CLUSTER_NAME="Enter EKS cluster name: "
set /p IMAGE_URI="Enter full image URI (e.g., 123456789012.dkr.ecr.us-east-1.amazonaws.com/doctor-patient-portal:latest): "

echo.
echo === Enter environment variable values ===
echo (Press Enter to skip a variable - it will use the placeholder from deployment.yaml)
echo.

set /p DB_HOST="Enter value for DB_HOST (or press Enter to skip): "
set /p DB_PORT="Enter value for DB_PORT (or press Enter to skip): "
set /p DB_NAME="Enter value for DB_NAME (or press Enter to skip): "
set /p DB_USER="Enter value for DB_USER (or press Enter to skip): "
set /p REDIS_HOST="Enter value for REDIS_HOST (or press Enter to skip): "
set /p REDIS_PORT="Enter value for REDIS_PORT (or press Enter to skip): "

echo.
echo === Credentials (entered without echo) ===
set /p DB_PASSWORD="Enter DB_PASSWORD: "
echo.
set /p REDIS_PASSWORD="Enter REDIS_PASSWORD: "
echo.

echo.
echo === Configuring kubectl for EKS cluster ===
aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!

echo.
echo === Checking cluster connectivity ===
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo kubectl cluster-info failed
    exit /b 1
)

echo.
echo === Applying namespace ===
kubectl apply -f kubernetes/namespace.yaml

echo.
echo === Creating Kubernetes Secret for credentials ===
set "SECRET_ARGS="
if not "!DB_PASSWORD!"=="" (
    set "SECRET_ARGS=!SECRET_ARGS! --from-literal=DB_PASSWORD=""!DB_PASSWORD!"""
)
if not "!REDIS_PASSWORD!"=="" (
    set "SECRET_ARGS=!SECRET_ARGS! --from-literal=REDIS_PASSWORD=""!REDIS_PASSWORD!"""
)

if not "!SECRET_ARGS!"=="" (
    kubectl create secret generic doctor-patient-portal-secrets -n doctor-patient-portal !SECRET_ARGS! --dry-run=client -o yaml | kubectl apply -f -
)

echo.
echo === Deploying application ===
copy kubernetes\deployment.yaml kubernetes\deployment.yaml.bak >nul

powershell -command "(gc kubernetes\deployment.yaml) -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Out-File -encoding ASCII kubernetes\deployment.yaml"
powershell -command "(gc kubernetes\deployment.yaml) -replace '{{DB_HOST}}', '!DB_HOST!' | Out-File -encoding ASCII kubernetes\deployment.yaml"
powershell -command "(gc kubernetes\deployment.yaml) -replace '{{DB_PORT}}', '!DB_PORT!' | Out-File -encoding ASCII kubernetes\deployment.yaml"
powershell -command "(gc kubernetes\deployment.yaml) -replace '{{DB_NAME}}', '!DB_NAME!' | Out-File -encoding ASCII kubernetes\deployment.yaml"
powershell -command "(gc kubernetes\deployment.yaml) -replace '{{DB_USER}}', '!DB_USER!' | Out-File -encoding ASCII kubernetes\deployment.yaml"
powershell -command "(gc kubernetes\deployment.yaml) -replace '{{REDIS_HOST}}', '!REDIS_HOST!' | Out-File -encoding ASCII kubernetes\deployment.yaml"
powershell -command "(gc kubernetes\deployment.yaml) -replace '{{REDIS_PORT}}', '!REDIS_PORT!' | Out-File -encoding ASCII kubernetes\deployment.yaml"

kubectl apply -f kubernetes/deployment.yaml

echo.
echo === Applying service ===
kubectl apply -f kubernetes/service.yaml

echo.
echo === Applying ingress ===
kubectl apply -f kubernetes/ingress.yaml

echo.
echo === Waiting for deployment rollout ===
kubectl rollout status deployment/doctor-patient-portal -n doctor-patient-portal

echo.
echo === Deployed resources ===
kubectl get pods,svc,ingress -n doctor-patient-portal

echo.
echo === Application URL ===
kubectl get ingress doctor-patient-portal-ingress -n doctor-patient-portal -o jsonpath="{.status.loadBalancer.ingress[0].hostname}"

echo.
echo.
echo === Deployment complete ===
echo To rollback on failure: kubectl rollout undo deployment/doctor-patient-portal -n doctor-patient-portal

copy /Y kubernetes\deployment.yaml.bak kubernetes\deployment.yaml >nul
del kubernetes\deployment.yaml.bak
