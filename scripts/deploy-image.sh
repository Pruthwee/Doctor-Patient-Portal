#!/bin/bash
set -e
set -o pipefail

PROJECT_NAME="Doctor-Patient-Portal"
NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

echo "=== Deploy Image to AWS EKS: $NAME ==="

# Ask for AWS region
read -p "Enter AWS region (e.g., us-east-1): " AWS_REGION

# Ask for EKS cluster name
read -p "Enter EKS cluster name: " CLUSTER_NAME

# Ask for image URI
read -p "Enter full image URI (registry/repo:tag): " IMAGE_URI

# Ask for environment variables (non-credential)
read -p "Enter DB_HOST (or press Enter to skip): " DB_HOST
read -p "Enter DB_PORT (or press Enter to skip): " DB_PORT
read -p "Enter DB_NAME (or press Enter to skip): " DB_NAME
read -p "Enter REDIS_HOST (or press Enter to skip): " REDIS_HOST
read -p "Enter REDIS_PORT (or press Enter to skip): " REDIS_PORT

# Ask for credentials without echoing
read -p "Enter DB_USER (or press Enter to skip): " DB_USER
read -s -p "Enter DB_PASSWORD (or press Enter to skip): " DB_PASSWORD
echo ""
read -s -p "Enter REDIS_PASSWORD (or press Enter to skip): " REDIS_PASSWORD
echo ""

echo "=== Configuring kubectl for EKS cluster ==="
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

echo "=== Checking cluster connectivity ==="
kubectl cluster-info || exit 1

echo "=== Applying namespace ==="
kubectl apply -f kubernetes/namespace.yaml

echo "=== Creating Kubernetes Secret ==="
SECRET_ARGS=""
if [ -n "$DB_USER" ]; then
    SECRET_ARGS="$SECRET_ARGS --from-literal=db-user=$DB_USER"
fi
if [ -n "$DB_PASSWORD" ]; then
    SECRET_ARGS="$SECRET_ARGS --from-literal=db-password=$DB_PASSWORD"
fi
if [ -n "$REDIS_PASSWORD" ]; then
    SECRET_ARGS="$SECRET_ARGS --from-literal=redis-password=$REDIS_PASSWORD"
fi

if [ -n "$SECRET_ARGS" ]; then
    kubectl create secret generic "$NAME-secrets" -n "$NAME" $SECRET_ARGS --dry-run=client -o yaml | kubectl apply -f -
fi

echo "=== Replacing placeholders in deployment.yaml ==="
sed -i.bak "s|{{IMAGE_URI}}|${IMAGE_URI}|g" kubernetes/deployment.yaml
sed -i.bak "s|{{NAMESPACE}}|${NAME}|g" kubernetes/deployment.yaml
sed -i.bak "s|{{DB_HOST}}|${DB_HOST}|g" kubernetes/deployment.yaml
sed -i.bak "s|{{DB_PORT}}|${DB_PORT}|g" kubernetes/deployment.yaml
sed -i.bak "s|{{DB_NAME}}|${DB_NAME}|g" kubernetes/deployment.yaml
sed -i.bak "s|{{REDIS_HOST}}|${REDIS_HOST}|g" kubernetes/deployment.yaml
sed -i.bak "s|{{REDIS_PORT}}|${REDIS_PORT}|g" kubernetes/deployment.yaml
# Remove backup files created by sed
rm -f kubernetes/deployment.yaml.bak

echo "=== Applying deployment ==="
kubectl apply -f kubernetes/deployment.yaml

echo "=== Applying service ==="
kubectl apply -f kubernetes/service.yaml

echo "=== Applying ingress ==="
kubectl apply -f kubernetes/ingress.yaml

echo "=== Waiting for deployment rollout ==="
kubectl rollout status deployment/"$NAME" -n "$NAME"

echo "=== Deployment status ==="
kubectl get pods,svc,ingress -n "$NAME"

# Get the application URL from the ingress
APP_URL=$(kubectl get ingress "$NAME-ingress" -n "$NAME" -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
if [ -z "$APP_URL" ]; then
    APP_URL=$(kubectl get ingress "$NAME-ingress" -n "$NAME" -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
fi
echo "=== Application URL: http://$APP_URL ==="

echo "=== Deployment complete ==="
echo "If the deployment fails, rollback with:"
echo "  kubectl rollout undo deployment/$NAME -n $NAME"
