#!/bin/bash
set -e

PROJECT_NAME="Doctor-Patient-Portal"
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

echo "Select Registry:"
echo "1. AWS ECR"
echo "2. Docker Hub"
read -p "Choice [1-2]: " REGISTRY_CHOICE

read -p "Enter Image Tag (default: latest): " IMAGE_TAG
IMAGE_TAG=${IMAGE_TAG:-latest}

if [ "$REGISTRY_CHOICE" == "1" ]; then
    read -p "Enter AWS Region: " AWS_REGION
    read -p "Enter ECR Repository Name: " ECR_REPO
    
    REGISTRY_URL=$(aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $(aws ecr get-registry-for-user --region $AWS_REGION) && echo $(aws ecr get-registry-for-user --region $AWS_REGION))
    
    aws ecr describe-repositories --repository-names $ECR_REPO --region $AWS_REGION >/dev/null 2>&1 || aws ecr create-repository --repository-name $ECR_REPO --region $AWS_REGION
    
    FULL_IMAGE_NAME="$REGISTRY_URL/$ECR_REPO:$IMAGE_TAG"
elif [ "$REGISTRY_CHOICE" == "2" ]; then
    read -p "Enter Docker Hub Username: " DOCKER_USERNAME
    read -s -p "Enter Docker Hub Password: " DOCKER_PASSWORD
    echo ""
    echo "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USERNAME" --password-stdin
    
    FULL_IMAGE_NAME="$DOCKER_USERNAME/$IMAGE_NAME:$IMAGE_TAG"
else
    echo "Invalid choice"
    exit 1
fi

echo "Building image: $FULL_IMAGE_NAME"
docker build -t "$FULL_IMAGE_NAME" .

echo "Pushing image: $FULL_IMAGE_NAME"
docker push "$FULL_IMAGE_NAME"

echo "Successfully built and pushed $FULL_IMAGE_NAME"
