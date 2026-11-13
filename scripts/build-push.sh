#!/bin/bash
set -e
set -o pipefail

echo "=== Build and Push Script for doctor-patient-portal ==="

# Project name
PROJECT_NAME="Doctor-Patient-Portal"
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

# Ask for target platform
echo ""
echo "Select target platform:"
echo "1) AWS ECR"
echo "2) Docker Hub"
read -p "Enter choice (1 or 2): " PLATFORM_CHOICE

if [ "$PLATFORM_CHOICE" = "1" ]; then
    # AWS ECR
    read -p "Enter AWS region (e.g., us-east-1): " AWS_REGION
    read -p "Enter ECR registry URL (e.g., 123456789012.dkr.ecr.us-east-1.amazonaws.com): " REGISTRY_URL
    read -p "Enter ECR repository name: " ECR_REPO
    read -p "Enter image tag (press Enter for 'latest'): " IMAGE_TAG

    if [ -z "$IMAGE_TAG" ]; then
        IMAGE_TAG="latest"
    fi

    # Sanitize tag
    IMAGE_TAG=$(echo "$IMAGE_TAG" | tr -cs 'a-z0-9-.' '-' | sed 's/^-*//;s/-*$//')
    if [ -z "$IMAGE_TAG" ]; then
        IMAGE_TAG="latest"
    fi

    FULL_IMAGE_NAME="${REGISTRY_URL}/${ECR_REPO}:${IMAGE_TAG}"

    echo ""
    echo "=== Building Docker image ==="
    echo "Image: $FULL_IMAGE_NAME"
    docker build -f Dockerfile -t "$FULL_IMAGE_NAME" .

    echo ""
    echo "=== Logging in to AWS ECR ==="
    aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$REGISTRY_URL"

    echo ""
    echo "=== Creating ECR repository if it does not exist ==="
    aws ecr describe-repositories --repository-names "$ECR_REPO" --region "$AWS_REGION" >/dev/null 2>&1 || \
        aws ecr create-repository --repository-name "$ECR_REPO" --region "$AWS_REGION"

    echo ""
    echo "=== Pushing Docker image ==="
    docker push "$FULL_IMAGE_NAME"

    echo ""
    echo "=== Build and push complete ==="
    echo "Image pushed to: $FULL_IMAGE_NAME"

elif [ "$PLATFORM_CHOICE" = "2" ]; then
    # Docker Hub
    read -p "Enter Docker Hub username: " DOCKER_USERNAME
    read -s -p "Enter Docker Hub password: " DOCKER_PASSWORD
    echo ""
    read -p "Enter repository name (or press Enter to use '$IMAGE_NAME'): " REPO_NAME
    if [ -z "$REPO_NAME" ]; then
        REPO_NAME="$IMAGE_NAME"
    fi
    read -p "Enter image tag (press Enter for 'latest'): " IMAGE_TAG

    if [ -z "$IMAGE_TAG" ]; then
        IMAGE_TAG="latest"
    fi

    # Sanitize tag
    IMAGE_TAG=$(echo "$IMAGE_TAG" | tr -cs 'a-z0-9-.' '-' | sed 's/^-*//;s/-*$//')
    if [ -z "$IMAGE_TAG" ]; then
        IMAGE_TAG="latest"
    fi

    FULL_IMAGE_NAME="${DOCKER_USERNAME}/${REPO_NAME}:${IMAGE_TAG}"

    echo ""
    echo "=== Building Docker image ==="
    echo "Image: $FULL_IMAGE_NAME"
    docker build -f Dockerfile -t "$FULL_IMAGE_NAME" .

    echo ""
    echo "=== Logging in to Docker Hub ==="
    echo "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USERNAME" --password-stdin

    echo ""
    echo "=== Pushing Docker image ==="
    docker push "$FULL_IMAGE_NAME"

    echo ""
    echo "=== Build and push complete ==="
    echo "Image pushed to: $FULL_IMAGE_NAME"

else
    echo "Invalid choice. Exiting."
    exit 1
fi
