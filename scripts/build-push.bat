@echo off
setlocal enabledelayedexpansion

echo === Doctor Patient Portal - Build and Push to AWS ECR ===

set "PROJECT_NAME=Doctor-Patient-Portal"
for /f "delims=" %%i in ('powershell -command "$n = '%PROJECT_NAME%'.ToLower(); $n -creplace '[^a-z0-9]', '-'; $n -creplace '^-+|-+$', ''"') do set "IMAGE_NAME=%%i"

set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "

set /p ECR_REPO="Enter ECR repository name (default: %IMAGE_NAME%): "
if "%ECR_REPO%"=="" set "ECR_REPO=%IMAGE_NAME%"

set /p IMAGE_TAG="Enter image tag (default: latest): "
if "%IMAGE_TAG%"=="" set "IMAGE_TAG=latest"

echo.
echo Getting AWS account ID...
for /f "delims=" %%i in ('aws sts get-caller-identity --query Account --output text --region !AWS_REGION!') do set "AWS_ACCOUNT_ID=%%i"
set "REGISTRY_URL=!AWS_ACCOUNT_ID!.dkr.ecr.!AWS_REGION!.amazonaws.com"
set "FULL_IMAGE_NAME=!REGISTRY_URL!/!ECR_REPO!:!IMAGE_TAG!"

echo.
echo Image will be: !FULL_IMAGE_NAME!
echo.

echo Logging in to Amazon ECR...
call aws ecr get-login-password --region !AWS_REGION! | docker login --username AWS --password-stdin !REGISTRY_URL!
if !ERRORLEVEL! neq 0 (
    echo ECR login failed
    exit /b 1
)

echo Checking if ECR repository '!ECR_REPO!' exists...
call aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo Creating ECR repository...
    call aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION!
)

echo Building Docker image...
docker build -f Dockerfile -t !FULL_IMAGE_NAME! .
if !ERRORLEVEL! neq 0 (
    echo Docker build failed
    exit /b 1
)

echo Pushing Docker image to ECR...
docker push !FULL_IMAGE_NAME!
if !ERRORLEVEL! neq 0 (
    echo Docker push failed
    exit /b 1
)

echo.
echo === Build and push complete ===
echo Image URI: !FULL_IMAGE_NAME!
echo Use this URI when deploying with scripts/deploy-image.bat

endlocal
