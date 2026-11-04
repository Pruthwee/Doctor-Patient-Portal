@echo off
setlocal enabledelayedexpansion

set "PROJECT_NAME=Doctor-Patient-Portal"
for /f "usebackq delims=" %%i in (`powershell -Command "$n = '%PROJECT_NAME%'.ToLower(); $n = [regex]::Replace($n, '[^a-z0-9]+', '-'); $n = $n.Trim('-'); Write-Output $n"`) do set "NAME=%%i"

echo === Deploy Image to AWS EKS: !NAME! ===

REM Ask for AWS region
set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "

REM Ask for EKS cluster name
set /p CLUSTER_NAME="Enter EKS cluster name: "

REM Ask for image URI
set /p IMAGE_URI="Enter full image URI (registry/repo:tag): "

REM Ask for environment variables (non-credential)
set /p DB_HOST="Enter DB_HOST (or press Enter to skip): "
set /p DB_PORT="Enter DB_PORT (or press Enter to skip): "
set /p DB_NAME="Enter DB_NAME (or press Enter to skip): "
set /p REDIS_HOST="Enter REDIS_HOST (or press Enter to skip): "
set /p REDIS_PORT="Enter REDIS_PORT (or press Enter to skip): "

REM Ask for credentials without echoing
set /p DB_USER="Enter DB_USER (or press Enter to skip): "
powershell -Command "$p = read-host -AsSecureString 'Enter DB_PASSWORD (or press Enter to skip)'; $BSTR = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($p); $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($BSTR); [Runtime.InteropServices.Marshal]::FreeBSTR($p); if ($plain -eq '') { '' } else { $plain }" > dbpass.tmp
set /p DB_PASSWORD=<dbpass.tmp
del dbpass.tmp 2>nul
powershell -Command "$p = read-host -AsSecureString 'Enter REDIS_PASSWORD (or press Enter to skip)'; $BSTR = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($p); $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($BSTR); [Runtime.InteropServices.Marshal]::FreeBSTR($p); if ($plain -eq '') { '' } else { $plain }" > redispass.tmp
set /p REDIS_PASSWORD=<redispass.tmp
del redispass.tmp 2>nul

echo === Configuring kubectl for EKS cluster ===
call aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!
if !ERRORLEVEL! neq 0 (
    echo Failed to configure kubectl
    exit /b 1
)

echo === Checking cluster connectivity ===
call kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo Cluster connectivity check failed
    exit /b 1
)

echo === Applying namespace ===
call kubectl apply -f kubernetes/namespace.yaml
if !ERRORLEVEL! neq 0 (
    echo Namespace application failed
    exit /b 1
)

echo === Creating Kubernetes Secret ===
set "SECRET_ARGS="
if not "!DB_USER!"=="" set "SECRET_ARGS=!SECRET_ARGS! --from-literal=db-user=!DB_USER!"
if not "!DB_PASSWORD!"=="" set "SECRET_ARGS=!SECRET_ARGS! --from-literal=db-password=!DB_PASSWORD!"
if not "!REDIS_PASSWORD!"=="" set "SECRET_ARGS=!SECRET_ARGS! --from-literal=redis-password=!REDIS_PASSWORD!"

if not "!SECRET_ARGS!"=="" (
    call kubectl create secret generic !NAME!-secrets -n !NAME! !SECRET_ARGS! --dry-run=client -o yaml | kubectl apply -f -
    if !ERRORLEVEL! neq 0 (
        echo Secret creation failed
        exit /b 1
    )
)

echo === Replacing placeholders in deployment.yaml ===
powershell -Command "(gc kubernetes\deployment.yaml) -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Out-File -Encoding UTF8 kubernetes\deployment.yaml.tmp"
powershell -Command "(gc kubernetes\deployment.yaml.tmp) -replace '{{NAMESPACE}}', '!NAME!' | Out-File -Encoding UTF8 kubernetes\deployment.yaml.tmp2"
powershell -Command "(gc kubernetes\deployment.yaml.tmp2) -replace '{{DB_HOST}}', '!DB_HOST!' | Out-File -Encoding UTF8 kubernetes\deployment.yaml.tmp"
powershell -Command "(gc kubernetes\deployment.yaml.tmp) -replace '{{DB_PORT}}', '!DB_PORT!' | Out-File -Encoding UTF8 kubernetes\deployment.yaml.tmp2"
powershell -Command "(gc kubernetes\deployment.yaml.tmp2) -replace '{{DB_NAME}}', '!DB_NAME!' | Out-File -Encoding UTF8 kubernetes\deployment.yaml.tmp"
powershell -Command "(gc kubernetes\deployment.yaml.tmp) -replace '{{REDIS_HOST}}', '!REDIS_HOST!' | Out-File -Encoding UTF8 kubernetes\deployment.yaml.tmp2"
powershell -Command "(gc kubernetes\deployment.yaml.tmp2) -replace '{{REDIS_PORT}}', '!REDIS_PORT!' | Out-File -Encoding UTF8 kubernetes\deployment.yaml"
del kubernetes\deployment.yaml.tmp kubernetes\deployment.yaml.tmp2 2>nul

echo === Applying deployment ===
call kubectl apply -f kubernetes\deployment.yaml
if !ERRORLEVEL! neq 0 (
    echo Deployment application failed
    exit /b 1
)

echo === Applying service ===
call kubectl apply -f kubernetes\service.yaml
if !ERRORLEVEL! neq 0 (
    echo Service application failed
    exit /b 1
)

echo === Applying ingress ===
call kubectl apply -f kubernetes\ingress.yaml
if !ERRORLEVEL! neq 0 (
    echo Ingress application failed
    exit /b 1
)

echo === Waiting for deployment rollout ===
call kubectl rollout status deployment/!NAME! -n !NAME!
if !ERRORLEVEL! neq 0 (
    echo Rollout failed
    exit /b 1
)

echo === Deployment status ===
call kubectl get pods,svc,ingress -n !NAME!

echo === Deployment complete ===
echo If the deployment fails, rollback with:
echo   kubectl rollout undo deployment/!NAME! -n !NAME!

endlocal
