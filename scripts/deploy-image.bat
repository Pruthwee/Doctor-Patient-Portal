@echo off
setlocal enabledelayedexpansion

set PROJECT_NAME=doctor-patient-portal

echo AWS EKS Deployment
set /p AWS_REGION="Enter AWS Region: "
set /p CLUSTER_NAME="Enter EKS Cluster Name: "
set /p IMAGE_URI="Enter Docker Image URI: "

set /p DB_URL="Enter DB_URL (or press Enter to skip): "
set /p DB_USER="Enter DB_USER (or press Enter to skip): "
set /p DB_PASS="Enter DB_PASS (or press Enter to skip): "
set /p REDIS_HOST="Enter REDIS_HOST (or press Enter to skip): "
set /p REDIS_PORT="Enter REDIS_PORT (or press Enter to skip): "
set /p REDIS_PASSWORD="Enter REDIS_PASSWORD (or press Enter to skip): "

echo Updating manifests...
powershell -Command "(gc kubernetes/deployment.yaml) -replace '{{IMAGE_URI}}', '%IMAGE_URI%' | Out-File -encoding utf8 kubernetes/deployment.yaml"
powershell -Command "(gc kubernetes/deployment.yaml) -replace '{{DB_URL}}', '%DB_URL%' | Out-File -encoding utf8 kubernetes/deployment.yaml"
powershell -Command "(gc kubernetes/deployment.yaml) -replace '{{DB_USER}}', '%DB_USER%' | Out-File -encoding utf8 kubernetes/deployment.yaml"
powershell -Command "(gc kubernetes/deployment.yaml) -replace '{{DB_PASS}}', '%DB_PASS%' | Out-File -encoding utf8 kubernetes/deployment.yaml"
powershell -Command "(gc kubernetes/deployment.yaml) -replace '{{REDIS_HOST}}', '%REDIS_HOST%' | Out-File -encoding utf8 kubernetes/deployment.yaml"
powershell -Command "(gc kubernetes/deployment.yaml) -replace '{{REDIS_PORT}}', '%REDIS_PORT%' | Out-File -encoding utf8 kubernetes/deployment.yaml"
powershell -Command "(gc kubernetes/deployment.yaml) -replace '{{REDIS_PASSWORD}}', '%REDIS_PASSWORD%' | Out-File -encoding utf8 kubernetes/deployment.yaml"

echo Configuring kubectl...
aws eks update-kubeconfig --region %AWS_REGION% --name %CLUSTER_NAME%

echo Verifying cluster connectivity...
kubectl cluster-info
if %ERRORLEVEL% neq 0 (echo Cluster connectivity failed & exit /b 1)

echo Applying manifests...
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml

echo Waiting for rollout...
kubectl rollout status deployment/%PROJECT_NAME% -n %PROJECT_NAME%

echo Verifying resources...
kubectl get pods,svc,ingress -n %PROJECT_NAME%

echo Deployment complete. Application URL: http://doctor-patient-portal.example.com
