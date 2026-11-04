#!/bin/bash
set -e

PROJECT_NAME="Doctor-Patient-Portal"
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

echo "=== Build and Push Script for $IMAGE_NAME ==="

# Ask for registry type
echo "Select target registry:"
echo "1. AWS ECR"
echo "2. Docker Hub"
read -p "Enter choice (1 or 2): " REGISTRY_CHOICE

if [ "$REGISTRY_CHOICE" = "1" ]; then
    # AWS ECR
    read -p "Enter AWS region (e.g., us-east-1): " AWS_REGION
    read -p "Enter AWS ECR registry URL (e.g., 123456789012.dkr.ecr.us-east-1.amazonaws.com): " REGISTRY_URL
    read -p "Enter ECR repository name (or press Enter to use $IMAGE_NAME): " ECR_REPO
    if [ -z "$ECR_REPO" ]; then
        ECR_REPO="$IMAGE_NAME"
    fi
    read -p "Enter image tag (or press Enter for latest): " IMAGE_TAG
    if [ -z "$IMAGE_TAG" ]; then
        IMAGE_TAG="latest"
    fi

    FULL_IMAGE_NAME="$REGISTRY_URL/$ECR_REPO:$IMAGE_TAG"

    echo "=== Logging in to AWS ECR ==="
    aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$REGISTRY_URL"

    echo "=== Creating ECR repository if it does not exist ==="
    aws ecr describe-repositories --repository-names "$ECR_REPO" --region "$AWS_REGION" >/dev/null 2>&1 || \
        aws ecr create-repository --repository-name "$ECR_REPO" --region "$AWS_REGION"

    echo "=== Building Docker image ==="
    docker build -f Dockerfile -t "$FULL_IMAGE_NAME" .

    echo "=== Pushing Docker image ==="
    docker push "$FULL_IMAGE_NAME"

    echo "=== Build and push complete ==="
    echo "Image: $FULL_IMAGE_NAME"

elif [ "$REGISTRY_CHOICE" = "2" ]; then
    # Docker Hub
    read -p "Enter Docker Hub username: " DOCKER_USERNAME
    read -s -p "Enter Docker Hub password: " DOCKER_PASSWORD
    echo ""
    read -p "Enter repository name (or press Enter to use $IMAGE_NAME): " DOCKER_REPO
    if [ -z "$DOCKER_REPO" ]; then
        DOCKER_REPO="$IMAGE_NAME"
    fi
    read -p "Enter image tag (or press Enter for latest): " IMAGE_TAG
    if [ -z "$IMAGE_TAG" ]; then
        IMAGE_TAG="latest"
    fi

    FULL_IMAGE_NAME="$DOCKER_USERNAME/$DOCKER_REPO:$IMAGE_TAG"

    echo "=== Logging in to Docker Hub ==="
    echo "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USERNAME" --password-stdin

    echo "=== Building Docker image ==="
    docker build -f Dockerfile -t "$FULL_IMAGE_NAME" .

    echo "=== Pushing Docker image ==="
    docker push "$FULL_IMAGE_NAME"

    echo "=== Build and push complete ==="
    echo "Image: $FULL_IMAGE_NAME"

else
    echo "Error: Invalid choice. Please run again and select 1 or 2."
    exit 1
fi
