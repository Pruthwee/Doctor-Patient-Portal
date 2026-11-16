@echo off
setlocal enabledelayedexpansion
set -e

echo === Doctor Patient Portal - Deploy to AWS EKS ===

set "PROJECT_NAME=Doctor-Patient-Portal"
for /f "delims=" %%i in ('powershell -command "$n = '%PROJECT_NAME%'.ToLower(); $n -creplace '[^a-z0-9]', '-'; $n -creplace '^-+|-+$', ''"') do set "IMAGE_NAME=%%i"
set "NAMESPACE=!IMAGE_NAME!"

set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "
set /p CLUSTER_NAME="Enter EKS cluster name: "
set /p IMAGE_URI="Enter full image URI (e.g., 123456789012.dkr.ecr.us-east-1.amazonaws.com/doctor-patient-portal:latest): "

echo.
echo Enter values for environment variables (press Enter to skip and use placeholder):
set /p DB_HOST="Enter value for DB_HOST (or press Enter to skip): "
set /p DB_PORT="Enter value for DB_PORT (or press Enter to skip): "
set /p DB_NAME="Enter value for DB_NAME (or press Enter to skip): "
set /p DB_USER="Enter value for DB_USER (or press Enter to skip): "
set /p REDIS_HOST="Enter value for REDIS_HOST (or press Enter to skip): "
set /p REDIS_PORT="Enter value for REDIS_PORT (or press Enter to skip): "

echo.
echo Enter credential values (not echoed):
set /p DB_PASSWORD="Enter value for DB_PASSWORD (or press Enter to skip): "
set /p REDIS_PASSWORD="Enter value for REDIS_PASSWORD (or press Enter to skip): "

echo.
echo Configuring kubectl for EKS cluster '!CLUSTER_NAME!'...
call aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!
if !ERRORLEVEL! neq 0 (
    echo Failed to configure kubectl
    exit /b 1
)

echo Checking cluster connectivity...
call kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo Cluster connectivity check failed
    exit /b 1
)

echo Applying namespace...
call kubectl apply -f kubernetes/namespace.yaml
if !ERRORLEVEL! neq 0 (
    echo Namespace application failed
    exit /b 1
)

echo Creating Kubernetes Secret...
set "SECRET_ARGS="
if not "!DB_PASSWORD!"=="" (
    set "SECRET_ARGS=!SECRET_ARGS! --from-literal=DB_PASSWORD=!DB_PASSWORD!"
)
if not "!REDIS_PASSWORD!"=="" (
    set "SECRET_ARGS=!SECRET_ARGS! --from-literal=REDIS_PASSWORD=!REDIS_PASSWORD!"
)
if not "!SECRET_ARGS!"=="" (
    call kubectl create secret generic !NAMESPACE!-secrets -n !NAMESPACE! !SECRET_ARGS! --dry-run=client -o yaml | kubectl apply -f -
    if !ERRORLEVEL! neq 0 (
        echo Secret creation failed
        exit /b 1
    )
)

echo Preparing deployment manifest...
copy kubernetes\deployment.yaml kubernetes\deployment.yaml.tmp >nul
powershell -command "(gc kubernetes\deployment.yaml.tmp) -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Out-File -Encoding utf8 kubernetes\deployment.yaml.tmp"
powershell -command "(gc kubernetes\deployment.yaml.tmp) -replace '{{NAMESPACE}}', '!NAMESPACE!' | Out-File -Encoding utf8 kubernetes\deployment.yaml.tmp"
powershell -command "(gc kubernetes\deployment.yaml.tmp) -replace '{{DB_HOST}}', '!DB_HOST!' | Out-File -Encoding utf8 kubernetes\deployment.yaml.tmp"
powershell -command "(gc kubernetes\deployment.yaml.tmp) -replace '{{DB_PORT}}', '!DB_PORT!' | Out-File -Encoding utf8 kubernetes\deployment.yaml.tmp"
powershell -command "(gc kubernetes\deployment.yaml.tmp) -replace '{{DB_NAME}}', '!DB_NAME!' | Out-File -Encoding utf8 kubernetes\deployment.yaml.tmp"
powershell -command "(gc kubernetes\deployment.yaml.tmp) -replace '{{DB_USER}}', '!DB_USER!' | Out-File -Encoding utf8 kubernetes\deployment.yaml.tmp"
powershell -command "(gc kubernetes\deployment.yaml.tmp) -replace '{{REDIS_HOST}}', '!REDIS_HOST!' | Out-File -Encoding utf8 kubernetes\deployment.yaml.tmp"
powershell -command "(gc kubernetes\deployment.yaml.tmp) -replace '{{REDIS_PORT}}', '!REDIS_PORT!' | Out-File -Encoding utf8 kubernetes\deployment.yaml.tmp"
move /y kubernetes\deployment.yaml.tmp kubernetes\deployment.yaml >nul

echo Applying deployment...
call kubectl apply -f kubernetes\deployment.yaml
if !ERRORLEVEL! neq 0 (
    echo Deployment application failed
    exit /b 1
)

echo Applying service...
call kubectl apply -f kubernetes\service.yaml
if !ERRORLEVEL! neq 0 (
    echo Service application failed
    exit /b 1
)

echo Applying ingress...
call kubectl apply -f kubernetes\ingress.yaml
if !ERRORLEVEL! neq 0 (
    echo Ingress application failed
    exit /b 1
)

echo Waiting for deployment rollout...
call kubectl rollout status deployment\!NAMESPACE! -n !NAMESPACE!
if !ERRORLEVEL! neq 0 (
    echo Rollout failed
    exit /b 1
)

echo.
echo === Deployment complete ===
call kubectl get pods,svc,ingress -n !NAMESPACE!

echo.
echo Application URL: http://doctor-patient-portal.example.com
echo.
echo To rollback on failure:
echo   kubectl rollout undo deployment\!NAMESPACE! -n !NAMESPACE!

endlocal
