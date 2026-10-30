@echo off & setlocal enabledelayedexpansion
rem Builds the doctor-patient-portal image and pushes it to AWS ECR or Docker Hub.
rem Run from the repository root: scripts\build-push.bat

set "PROJECT_NAME=doctor-patient-portal"
set "IMAGE_NAME=!PROJECT_NAME!"

where docker >nul 2>&1
if !ERRORLEVEL! neq 0 (echo Docker is not installed & exit /b 1)

echo Select the registry
echo   1. AWS ECR (target platform AWS_EKS)
echo   2. Docker Hub
set /p REGISTRY_CHOICE="Enter 1 or 2: "

if "!REGISTRY_CHOICE!"=="1" (
    where aws >nul 2>&1
    if !ERRORLEVEL! neq 0 (echo The AWS CLI is not installed & exit /b 1)
    set /p AWS_REGION="Enter the AWS region (for example us-east-1): "
    set /p AWS_ACCOUNT_ID="Enter the AWS account ID: "
    set "ECR_REPO="
    set /p ECR_REPO="Enter the ECR repository name (press Enter for !IMAGE_NAME!): "
    if "!ECR_REPO!"=="" set "ECR_REPO=!IMAGE_NAME!"
    set "REGISTRY_URL=!AWS_ACCOUNT_ID!.dkr.ecr.!AWS_REGION!.amazonaws.com"
    set "REPO_PATH=!ECR_REPO!"
) else if "!REGISTRY_CHOICE!"=="2" (
    set /p DOCKER_USERNAME="Enter the Docker Hub username: "
    set /p DOCKER_PASSWORD="Enter the Docker Hub password or access token: "
    set "REGISTRY_URL=docker.io"
    set "REPO_PATH=!DOCKER_USERNAME!/!IMAGE_NAME!"
) else (
    echo Invalid choice, enter 1 or 2
    exit /b 1
)

set "IMAGE_TAG="
set /p IMAGE_TAG="Enter IMAGE_TAG (press Enter for latest): "
if "!IMAGE_TAG!"=="" (
    set "IMAGE_TAG=latest"
) else (
    powershell -NoProfile -Command "$t='!IMAGE_TAG!'.ToLower() -replace '[^a-z0-9._-]+','-'; $t.Trim('-')" > "%TEMP%\portal-image-tag.txt"
    if !ERRORLEVEL! neq 0 (echo Tag sanitizing failed & exit /b 1)
    set "IMAGE_TAG="
    set /p IMAGE_TAG=<"%TEMP%\portal-image-tag.txt"
    del "%TEMP%\portal-image-tag.txt" >nul 2>&1
)
if "!IMAGE_TAG!"=="" set "IMAGE_TAG=latest"

set "FULL_IMAGE_NAME=!REGISTRY_URL!/!REPO_PATH!:!IMAGE_TAG!"
echo Image to build and push !FULL_IMAGE_NAME!

echo Step 1 of 3 building the image...
docker build -f Dockerfile -t !FULL_IMAGE_NAME! .
if !ERRORLEVEL! neq 0 (echo Docker build failed & exit /b 1)

echo Step 2 of 3 logging in to the registry...
if "!REGISTRY_CHOICE!"=="1" (
    aws ecr get-login-password --region !AWS_REGION! | docker login --username AWS --password-stdin !REGISTRY_URL!
    if !ERRORLEVEL! neq 0 (echo ECR login failed & exit /b 1)
    aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
    if !ERRORLEVEL! neq 0 (echo Creating ECR repository... & aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION!)
) else (
    echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
    if !ERRORLEVEL! neq 0 (echo Docker Hub login failed & exit /b 1)
)

echo Step 3 of 3 pushing the image...
docker push !FULL_IMAGE_NAME!
if !ERRORLEVEL! neq 0 (echo Docker push failed & exit /b 1)

echo Done. Pushed image !FULL_IMAGE_NAME!
echo Use this image URI in scripts\deploy-image.bat
endlocal
