#!/bin/bash
set -e
set -o pipefail

echo "=== Doctor Patient Portal - Deploy to AWS EKS ==="

# Project name
PROJECT_NAME="Doctor-Patient-Portal"
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')
NAMESPACE="$IMAGE_NAME"

# Ask for AWS region
read -p "Enter AWS region (e.g., us-east-1): " AWS_REGION

# Ask for EKS cluster name
read -p "Enter EKS cluster name: " CLUSTER_NAME

# Ask for image URI
read -p "Enter full image URI (e.g., 123456789012.dkr.ecr.us-east-1.amazonaws.com/doctor-patient-portal:latest): " IMAGE_URI

# Ask for environment variable values
echo ""
echo "Enter values for environment variables (press Enter to skip and use placeholder):"
read -p "Enter value for DB_HOST (or press Enter to skip): " DB_HOST
read -p "Enter value for DB_PORT (or press Enter to skip): " DB_PORT
read -p "Enter value for DB_NAME (or press Enter to skip): " DB_NAME
read -p "Enter value for DB_USER (or press Enter to skip): " DB_USER
read -p "Enter value for REDIS_HOST (or press Enter to skip): " REDIS_HOST
read -p "Enter value for REDIS_PORT (or press Enter to skip): " REDIS_PORT

# Ask for secret values (without echoing)
echo ""
echo "Enter credential values (not echoed):"
read -s -p "Enter value for DB_PASSWORD (or press Enter to skip): " DB_PASSWORD
echo ""
read -s -p "Enter value for REDIS_PASSWORD (or press Enter to skip): " REDIS_PASSWORD
echo ""

# Configure kubectl for EKS
echo ""
echo "Configuring kubectl for EKS cluster '$CLUSTER_NAME'..."
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

# Verify cluster connectivity
echo "Checking cluster connectivity..."
kubectl cluster-info || exit 1

# Apply namespace
echo "Applying namespace..."
kubectl apply -f kubernetes/namespace.yaml

# Create Secret with credentials
echo "Creating Kubernetes Secret..."
SECRET_ARGS=""
if [ -n "$DB_PASSWORD" ]; then
    SECRET_ARGS="$SECRET_ARGS --from-literal=DB_PASSWORD=\"$DB_PASSWORD\""
fi
if [ -n "$REDIS_PASSWORD" ]; then
    SECRET_ARGS="$SECRET_ARGS --from-literal=REDIS_PASSWORD=\"$REDIS_PASSWORD\""
fi
if [ -n "$SECRET_ARGS" ]; then
    kubectl create secret generic "$NAMESPACE-secrets" -n "$NAMESPACE" $SECRET_ARGS --dry-run=client -o yaml | kubectl apply -f -
fi

# Prepare deployment.yaml with placeholder substitutions
echo "Preparing deployment manifest..."
cp kubernetes/deployment.yaml "$JOB_WORK_DIR/deployment.yaml.bak" 2>/dev/null || cp kubernetes/deployment.yaml /tmp/deployment.yaml.bak
sed -i.bak 's|{{IMAGE_URI}}|'"$IMAGE_URI"'|g' kubernetes/deployment.yaml
sed -i.bak 's|{{NAMESPACE}}|'"$NAMESPACE"'|g' kubernetes/deployment.yaml
sed -i.bak 's|{{DB_HOST}}|'"${DB_HOST:-}"'|g' kubernetes/deployment.yaml
sed -i.bak 's|{{DB_PORT}}|'"${DB_PORT:-}"'|g' kubernetes/deployment.yaml
sed -i.bak 's|{{DB_NAME}}|'"${DB_NAME:-}"'|g' kubernetes/deployment.yaml
sed -i.bak 's|{{DB_USER}}|'"${DB_USER:-}"'|g' kubernetes/deployment.yaml
sed -i.bak 's|{{REDIS_HOST}}|'"${REDIS_HOST:-}"'|g' kubernetes/deployment.yaml
sed -i.bak 's|{{REDIS_PORT}}|'"${REDIS_PORT:-}"'|g' kubernetes/deployment.yaml
rm -f kubernetes/deployment.yaml.bak

# Apply deployment
echo "Applying deployment..."
kubectl apply -f kubernetes/deployment.yaml

# Apply service
echo "Applying service..."
kubectl apply -f kubernetes/service.yaml

# Apply ingress
echo "Applying ingress..."
kubectl apply -f kubernetes/ingress.yaml

# Wait for rollout
echo "Waiting for deployment rollout..."
kubectl rollout status deployment/"$NAMESPACE" -n "$NAMESPACE"

# Show resources
echo ""
echo "=== Deployment complete ==="
kubectl get pods,svc,ingress -n "$NAMESPACE"

# Show application URL
echo ""
echo "Application URL: http://doctor-patient-portal.example.com"
echo ""
echo "To rollback on failure:"
echo "  kubectl rollout undo deployment/$NAMESPACE -n $NAMESPACE"
