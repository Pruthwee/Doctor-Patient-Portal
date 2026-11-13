#!/bin/bash
set -e
set -o pipefail

echo "=== Deploy doctor-patient-portal to AWS EKS ==="

# Ask for AWS region and cluster name
read -p "Enter AWS region (e.g., us-east-1): " AWS_REGION
read -p "Enter EKS cluster name: " CLUSTER_NAME
read -p "Enter full image URI (e.g., 123456789012.dkr.ecr.us-east-1.amazonaws.com/doctor-patient-portal:latest): " IMAGE_URI

# Ask for environment variables
echo ""
echo "=== Enter environment variable values ==="
echo "(Press Enter to skip a variable — it will use the placeholder from deployment.yaml)"
echo ""

read -p "Enter value for DB_HOST (or press Enter to skip): " DB_HOST
read -p "Enter value for DB_PORT (or press Enter to skip): " DB_PORT
read -p "Enter value for DB_NAME (or press Enter to skip): " DB_NAME
read -p "Enter value for DB_USER (or press Enter to skip): " DB_USER
read -p "Enter value for REDIS_HOST (or press Enter to skip): " REDIS_HOST
read -p "Enter value for REDIS_PORT (or press Enter to skip): " REDIS_PORT

# Ask for credentials (without echoing)
echo ""
echo "=== Credentials (entered without echo) ==="
read -s -p "Enter DB_PASSWORD: " DB_PASSWORD
echo ""
read -s -p "Enter REDIS_PASSWORD: " REDIS_PASSWORD
echo ""

# Configure kubectl for the EKS cluster
echo ""
echo "=== Configuring kubectl for EKS cluster ==="
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

echo ""
echo "=== Checking cluster connectivity ==="
kubectl cluster-info || exit 1

# Apply namespace
echo ""
echo "=== Applying namespace ==="
kubectl apply -f kubernetes/namespace.yaml

# Create Kubernetes Secret for credentials
echo ""
echo "=== Creating Kubernetes Secret for credentials ==="
SECRET_ARGS=""
if [ -n "$DB_PASSWORD" ]; then
    SECRET_ARGS="$SECRET_ARGS --from-literal=DB_PASSWORD=\"$DB_PASSWORD\""
fi
if [ -n "$REDIS_PASSWORD" ]; then
    SECRET_ARGS="$SECRET_ARGS --from-literal=REDIS_PASSWORD=\"$REDIS_PASSWORD\""
fi

if [ -n "$SECRET_ARGS" ]; then
    kubectl create secret generic doctor-patient-portal-secrets -n doctor-patient-portal $SECRET_ARGS --dry-run=client -o yaml | kubectl apply -f -
fi

# Replace placeholders in deployment.yaml and apply
echo ""
echo "=== Deploying application ==="
cp kubernetes/deployment.yaml /tmp/deployment.yaml.bak

sed -i.bak "s|{{IMAGE_URI}}|$IMAGE_URI|g" kubernetes/deployment.yaml
sed -i.bak "s|{{DB_HOST}}|$DB_HOST|g" kubernetes/deployment.yaml
sed -i.bak "s|{{DB_PORT}}|$DB_PORT|g" kubernetes/deployment.yaml
sed -i.bak "s|{{DB_NAME}}|$DB_NAME|g" kubernetes/deployment.yaml
sed -i.bak "s|{{DB_USER}}|$DB_USER|g" kubernetes/deployment.yaml
sed -i.bak "s|{{REDIS_HOST}}|$REDIS_HOST|g" kubernetes/deployment.yaml
sed -i.bak "s|{{REDIS_PORT}}|$REDIS_PORT|g" kubernetes/deployment.yaml

kubectl apply -f kubernetes/deployment.yaml

# Apply service and ingress
echo ""
echo "=== Applying service ==="
kubectl apply -f kubernetes/service.yaml

echo ""
echo "=== Applying ingress ==="
kubectl apply -f kubernetes/ingress.yaml

# Wait for deployment to complete
echo ""
echo "=== Waiting for deployment rollout ==="
kubectl rollout status deployment/doctor-patient-portal -n doctor-patient-portal

# Restore original deployment.yaml
mv /tmp/deployment.yaml.bak kubernetes/deployment.yaml

# Show resources
echo ""
echo "=== Deployed resources ==="
kubectl get pods,svc,ingress -n doctor-patient-portal

echo ""
echo "=== Application URL ==="
kubectl get ingress doctor-patient-portal-ingress -n doctor-patient-portal -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'

echo ""
echo ""
echo "=== Deployment complete ==="
echo "To rollback on failure: kubectl rollout undo deployment/doctor-patient-portal -n doctor-patient-portal"
