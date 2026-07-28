#!/bin/bash
set -e
set -o pipefail

PROJECT_NAME="doctor-patient-portal"

echo "AWS EKS Deployment"
read -p "Enter AWS Region: " AWS_REGION
read -p "Enter EKS Cluster Name: " CLUSTER_NAME
read -p "Enter Docker Image URI (e.g., 123456789012.dkr.ecr.us-east-1.amazonaws.com/repo:tag): " IMAGE_URI

# Application specific environment variables
read -p "Enter DB_URL (or press Enter to skip): " DB_URL
read -p "Enter DB_USER (or press Enter to skip): " DB_USER
read -p "Enter DB_PASS (or press Enter to skip): " DB_PASS
read -p "Enter REDIS_HOST (or press Enter to skip): " REDIS_HOST
read -p "Enter REDIS_PORT (or press Enter to skip): " REDIS_PORT
read -p "Enter REDIS_PASSWORD (or press Enter to skip): " REDIS_PASSWORD

# Update manifests
sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" kubernetes/deployment.yaml
sed -i "s|{{DB_URL}}|$DB_URL|g" kubernetes/deployment.yaml
sed -i "s|{{DB_USER}}|$DB_USER|g" kubernetes/deployment.yaml
sed -i "s|{{DB_PASS}}|$DB_PASS|g" kubernetes/deployment.yaml
sed -i "s|{{REDIS_HOST}}|$REDIS_HOST|g" kubernetes/deployment.yaml
sed -i "s|{{REDIS_PORT}}|$REDIS_PORT|g" kubernetes/deployment.yaml
sed -i "s|{{REDIS_PASSWORD}}|$REDIS_PASSWORD|g" kubernetes/deployment.yaml

echo "Configuring kubectl..."
aws eks update-kubeconfig --region $AWS_REGION --name $CLUSTER_NAME

echo "Verifying cluster connectivity..."
kubectl cluster-info || { echo "Cluster connectivity failed"; exit 1; }

echo "Applying manifests..."
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml

echo "Waiting for rollout..."
kubectl rollout status deployment/$PROJECT_NAME -n $PROJECT_NAME

echo "Verifying resources..."
kubectl get pods,svc,ingress -n $PROJECT_NAME

echo "Deployment complete. Application URL: http://doctor-patient-portal.example.com"
