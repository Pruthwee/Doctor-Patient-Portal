@echo off
setlocal enabledelayedexpansion

set "PROJECT_NAME=Doctor-Patient-Portal"
REM Sanitize IMAGE_NAME: lowercase, non-alphanumeric to hyphen, no leading/trailing hyphen
for /f "usebackq delims=" %%i in (`powershell -Command "$n = '%PROJECT_NAME%'.ToLower(); $n = [regex]::Replace($n, '[^a-z0-9]+', '-'); $n = $n.Trim('-'); Write-Output $n"`) do set "IMAGE_NAME=%%i"

echo === Build and Push Script for !IMAGE_NAME! ===

REM Ask for registry type
echo Select target registry:
echo 1. AWS ECR
echo 2. Docker Hub
set /p REGISTRY_CHOICE="Enter choice (1 or 2): "

if "!REGISTRY_CHOICE!"=="1" (
    REM AWS ECR
    set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "
    set /p REGISTRY_URL="Enter AWS ECR registry URL (e.g., 123456789012.dkr.ecr.us-east-1.amazonaws.com): "
    set /p ECR_REPO="Enter ECR repository name (or press Enter to use !IMAGE_NAME!): "
    if "!ECR_REPO!"=="" set "ECR_REPO=!IMAGE_NAME!"
    set /p IMAGE_TAG="Enter image tag (or press Enter for latest): "
    if "!IMAGE_TAG!"=="" set "IMAGE_TAG=latest"

    set "FULL_IMAGE_NAME=!REGISTRY_URL!/!ECR_REPO!:!IMAGE_TAG!"

    echo === Logging in to AWS ECR ===
    call aws ecr get-login-password --region !AWS_REGION! | docker login --username AWS --password-stdin !REGISTRY_URL!
    if !ERRORLEVEL! neq 0 (
        echo ECR login failed
        exit /b 1
    )

    echo === Creating ECR repository if it does not exist ===
    call aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
    if !ERRORLEVEL! neq 0 (
        echo Creating ECR repository...
        call aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION!
    )

    echo === Building Docker image ===
    docker build -f Dockerfile -t "!FULL_IMAGE_NAME!" .
    if !ERRORLEVEL! neq 0 (
        echo Docker build failed
        exit /b 1
    )

    echo === Pushing Docker image ===
    docker push "!FULL_IMAGE_NAME!"
    if !ERRORLEVEL! neq 0 (
        echo Docker push failed
        exit /b 1
    )

    echo === Build and push complete ===
    echo Image: !FULL_IMAGE_NAME!

) else if "!REGISTRY_CHOICE!"=="2" (
    REM Docker Hub
    set /p DOCKER_USERNAME="Enter Docker Hub username: "
    powershell -Command "$p = read-host -AsSecureString 'Enter Docker Hub password'; $BSTR = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($p); $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($BSTR); [Runtime.InteropServices.Marshal]::FreeBSTR($p); $plain" > dockpass.tmp
    set /p DOCKER_PASSWORD=<dockpass.tmp
    del dockpass.tmp 2>nul
    set /p DOCKER_REPO="Enter repository name (or press Enter to use !IMAGE_NAME!): "
    if "!DOCKER_REPO!"=="" set "DOCKER_REPO=!IMAGE_NAME!"
    set /p IMAGE_TAG="Enter image tag (or press Enter for latest): "
    if "!IMAGE_TAG!"=="" set "IMAGE_TAG=latest"

    set "FULL_IMAGE_NAME=!DOCKER_USERNAME!/!DOCKER_REPO!:!IMAGE_TAG!"

    echo === Logging in to Docker Hub ===
    echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
    if !ERRORLEVEL! neq 0 (
        echo Docker Hub login failed
        exit /b 1
    )

    echo === Building Docker image ===
    docker build -f Dockerfile -t "!FULL_IMAGE_NAME!" .
    if !ERRORLEVEL! neq 0 (
        echo Docker build failed
        exit /b 1
    )

    echo === Pushing Docker image ===
    docker push "!FULL_IMAGE_NAME!"
    if !ERRORLEVEL! neq 0 (
        echo Docker push failed
        exit /b 1
    )

    echo === Build and push complete ===
    echo Image: !FULL_IMAGE_NAME!

) else (
    echo Error: Invalid choice. Please run again and select 1 or 2.
    exit /b 1
)

endlocal
