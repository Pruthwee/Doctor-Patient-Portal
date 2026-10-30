@echo off & setlocal enabledelayedexpansion
rem Deploys the pushed doctor-patient-portal image to AWS EKS with kubectl.
rem Run from the repository root: scripts\deploy-image.bat

set "APP_NAME=doctor-patient-portal"
set "NAMESPACE=doctor-patient-portal"
set "DEPLOYMENT_FILE=kubernetes\deployment.yaml"
set "ORIGINAL_DEPLOYMENT=%TEMP%\portal-deployment-original.yaml"

where aws >nul 2>&1
if !ERRORLEVEL! neq 0 (echo The AWS CLI is not installed & exit /b 1)
where kubectl >nul 2>&1
if !ERRORLEVEL! neq 0 (echo kubectl is not installed & exit /b 1)

set /p AWS_REGION="Enter the AWS region (for example us-east-1): "
set /p CLUSTER_NAME="Enter the EKS cluster name: "
set /p IMAGE_URI="Enter the image URI (registry/repository and tag): "

if "!AWS_REGION!"=="" (echo The region is required & exit /b 1)
if "!CLUSTER_NAME!"=="" (echo The cluster name is required & exit /b 1)
if "!IMAGE_URI!"=="" (echo The image URI is required & exit /b 1)

set "REDIS_HOST="
set "REDIS_PORT="
set "REDIS_SSL="
set "REDIS_PASSWORD="
set /p REDIS_HOST="Enter value for REDIS_HOST, the ElastiCache endpoint (or press Enter to skip): "
set /p REDIS_PORT="Enter value for REDIS_PORT (or press Enter to skip, default 6379): "
set /p REDIS_SSL="Enter value for REDIS_SSL, true when in-transit encryption is on (or press Enter to skip, default false): "
set /p REDIS_PASSWORD="Enter value for REDIS_PASSWORD, the Redis AUTH token (or press Enter to skip, it is shown while typing): "
if "!REDIS_PORT!"=="" set "REDIS_PORT=6379"
if "!REDIS_SSL!"=="" set "REDIS_SSL=false"

echo Configuring kubectl for cluster !CLUSTER_NAME! in !AWS_REGION!...
aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!
if !ERRORLEVEL! neq 0 (echo aws eks update-kubeconfig failed & exit /b 1)
kubectl cluster-info
if !ERRORLEVEL! neq 0 (echo Cannot reach the cluster & exit /b 1)

rem Keep the template untouched, the original is restored at the end
copy /y "!DEPLOYMENT_FILE!" "!ORIGINAL_DEPLOYMENT!" >nul

echo Replacing the placeholders in !DEPLOYMENT_FILE!...
powershell -NoProfile -Command "(Get-Content '!DEPLOYMENT_FILE!' -Raw).Replace('{{IMAGE_URI}}','!IMAGE_URI!').Replace('{{NAMESPACE}}','!NAMESPACE!').Replace('{{REDIS_HOST}}','!REDIS_HOST!').Replace('{{REDIS_PORT}}','!REDIS_PORT!').Replace('{{REDIS_SSL}}','!REDIS_SSL!') | Set-Content '!DEPLOYMENT_FILE!'"
if !ERRORLEVEL! neq 0 (echo Placeholder replacement failed & goto :failed)

echo Applying the namespace...
kubectl apply -f kubernetes\namespace.yaml
if !ERRORLEVEL! neq 0 (goto :failed)

echo Creating the secret !APP_NAME!-secrets...
kubectl create secret generic !APP_NAME!-secrets -n !NAMESPACE! --from-literal=REDIS_PASSWORD="!REDIS_PASSWORD!" --dry-run=client -o yaml | kubectl apply -f -
if !ERRORLEVEL! neq 0 (goto :failed)

echo Applying the deployment, the service and the ingress...
kubectl apply -f kubernetes\deployment.yaml
if !ERRORLEVEL! neq 0 (goto :failed)
kubectl apply -f kubernetes\service.yaml
if !ERRORLEVEL! neq 0 (goto :failed)
kubectl apply -f kubernetes\ingress.yaml
if !ERRORLEVEL! neq 0 (goto :failed)

echo Waiting for the rollout...
kubectl rollout status deployment/!APP_NAME! -n !NAMESPACE! --timeout=300s
if !ERRORLEVEL! neq 0 (goto :failed)

echo Deployment state
kubectl get pods,svc,ingress -n !NAMESPACE!

echo The application URL is the ADDRESS of the ingress shown above
echo To roll back, run kubectl rollout undo deployment/!APP_NAME! -n !NAMESPACE!

copy /y "!ORIGINAL_DEPLOYMENT!" "!DEPLOYMENT_FILE!" >nul
del "!ORIGINAL_DEPLOYMENT!" >nul 2>&1
endlocal
exit /b 0

:failed
echo The deployment failed. To roll back, run kubectl rollout undo deployment/!APP_NAME! -n !NAMESPACE!
echo To inspect, run kubectl describe pods -n !NAMESPACE!
if exist "!ORIGINAL_DEPLOYMENT!" (
    copy /y "!ORIGINAL_DEPLOYMENT!" "!DEPLOYMENT_FILE!" >nul
    del "!ORIGINAL_DEPLOYMENT!" >nul 2>&1
)
exit /b 1
