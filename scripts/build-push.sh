#!/bin/bash
# Builds the doctor-patient-portal image and pushes it to AWS ECR or Docker Hub.
# Run from the repository root: ./scripts/build-push.sh
set -e

PROJECT_NAME="doctor-patient-portal"
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

command -v docker >/dev/null 2>&1 || { echo "ERROR: docker is not installed"; exit 1; }

echo "Select the registry:"
echo "  1. AWS ECR (target platform AWS_EKS)"
echo "  2. Docker Hub"
read -r -p "Enter 1 or 2: " REGISTRY_CHOICE

case "$REGISTRY_CHOICE" in
  1)
    command -v aws >/dev/null 2>&1 || { echo "ERROR: the AWS CLI is not installed"; exit 1; }
    read -r -p "Enter the AWS region (for example us-east-1): " AWS_REGION
    read -r -p "Enter the AWS account ID: " AWS_ACCOUNT_ID
    read -r -p "Enter the ECR repository name [$IMAGE_NAME]: " ECR_REPO
    ECR_REPO=${ECR_REPO:-$IMAGE_NAME}
    ECR_REPO=$(echo "$ECR_REPO" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')
    REGISTRY_URL="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
    REPO_PATH="$ECR_REPO"
    ;;
  2)
    read -r -p "Enter the Docker Hub username: " DOCKER_USERNAME
    read -r -s -p "Enter the Docker Hub password or access token: " DOCKER_PASSWORD
    echo
    REGISTRY_URL="docker.io"
    DOCKER_USERNAME=$(echo "$DOCKER_USERNAME" | tr '[:upper:]' '[:lower:]')
    REPO_PATH="${DOCKER_USERNAME}/${IMAGE_NAME}"
    ;;
  *)
    echo "ERROR: invalid choice, enter 1 or 2"
    exit 1
    ;;
esac

read -r -p "Enter IMAGE_TAG (press Enter for latest): " IMAGE_TAG
IMAGE_TAG=$(echo "$IMAGE_TAG" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9._-' '-' | sed 's/^-*//;s/-*$//')
IMAGE_TAG=${IMAGE_TAG:-latest}

FULL_IMAGE_NAME="${REGISTRY_URL}/${REPO_PATH}:${IMAGE_TAG}"
echo "Image to build and push: $FULL_IMAGE_NAME"

echo "Step 1/3: building the image..."
docker build -f Dockerfile -t "$FULL_IMAGE_NAME" .
echo "Build finished."

echo "Step 2/3: logging in to the registry..."
if [ "$REGISTRY_CHOICE" = "1" ]; then
  aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $REGISTRY_URL
  aws ecr describe-repositories --repository-names $ECR_REPO --region $AWS_REGION >/dev/null 2>&1 || aws ecr create-repository --repository-name $ECR_REPO --region $AWS_REGION
else
  echo "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USERNAME" --password-stdin
fi

echo "Step 3/3: pushing the image..."
docker push "$FULL_IMAGE_NAME"

echo "Done. Pushed image: $FULL_IMAGE_NAME"
echo "Use this image URI in scripts/deploy-image.sh"
