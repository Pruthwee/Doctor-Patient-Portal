#!/bin/bash
# Deploys the pushed doctor-patient-portal image to AWS EKS with kubectl.
# Run from the repository root: ./scripts/deploy-image.sh
set -e
set -o pipefail

APP_NAME="doctor-patient-portal"
NAMESPACE="doctor-patient-portal"
DEPLOYMENT_FILE="kubernetes/deployment.yaml"
ORIGINAL_DEPLOYMENT="$(mktemp)"

command -v aws >/dev/null 2>&1 || { echo "ERROR: the AWS CLI is not installed"; exit 1; }
command -v kubectl >/dev/null 2>&1 || { echo "ERROR: kubectl is not installed"; exit 1; }

# Keep the template untouched: the placeholders are replaced in place, and the original is restored on exit
cp "$DEPLOYMENT_FILE" "$ORIGINAL_DEPLOYMENT"
cleanup() {
  cp "$ORIGINAL_DEPLOYMENT" "$DEPLOYMENT_FILE" 2>/dev/null || true
  rm -f "$ORIGINAL_DEPLOYMENT" "$DEPLOYMENT_FILE.bak"
}
trap cleanup EXIT

rollback_help() {
  echo "The deployment failed. To roll back:"
  echo "  kubectl rollout undo deployment/$APP_NAME -n $NAMESPACE"
  echo "  kubectl rollout status deployment/$APP_NAME -n $NAMESPACE"
  echo "To inspect: kubectl describe pods -n $NAMESPACE ; kubectl logs deployment/$APP_NAME -n $NAMESPACE"
}

read -r -p "Enter the AWS region (for example us-east-1): " AWS_REGION
read -r -p "Enter the EKS cluster name: " CLUSTER_NAME
read -r -p "Enter the image URI (registry/repository:tag, for example 123456789012.dkr.ecr.us-east-1.amazonaws.com/$APP_NAME:latest): " IMAGE_URI

if [ -z "$AWS_REGION" ] || [ -z "$CLUSTER_NAME" ] || [ -z "$IMAGE_URI" ]; then
  echo "ERROR: the region, the cluster name and the image URI are required"
  exit 1
fi

read -r -p "Enter value for REDIS_HOST, the ElastiCache endpoint (or press Enter to skip): " REDIS_HOST
read -r -p "Enter value for REDIS_PORT (or press Enter to skip, default 6379): " REDIS_PORT
read -r -p "Enter value for REDIS_SSL, true when in-transit encryption is on (or press Enter to skip, default false): " REDIS_SSL
read -r -s -p "Enter value for REDIS_PASSWORD, the Redis AUTH token (hidden, or press Enter to skip): " REDIS_PASSWORD
echo
REDIS_PORT=${REDIS_PORT:-6379}
REDIS_SSL=${REDIS_SSL:-false}

echo "Configuring kubectl for cluster $CLUSTER_NAME in $AWS_REGION..."
aws eks update-kubeconfig --region $AWS_REGION --name $CLUSTER_NAME || { echo "ERROR: aws eks update-kubeconfig failed"; exit 1; }
kubectl cluster-info || exit 1

echo "Replacing the placeholders in $DEPLOYMENT_FILE..."
sed -i.bak 's|{{IMAGE_URI}}|'"$IMAGE_URI"'|g' $DEPLOYMENT_FILE
sed -i.bak 's|{{NAMESPACE}}|'"$NAMESPACE"'|g' $DEPLOYMENT_FILE
sed -i.bak 's|{{REDIS_HOST}}|'"$REDIS_HOST"'|g' $DEPLOYMENT_FILE
sed -i.bak 's|{{REDIS_PORT}}|'"$REDIS_PORT"'|g' $DEPLOYMENT_FILE
sed -i.bak 's|{{REDIS_SSL}}|'"$REDIS_SSL"'|g' $DEPLOYMENT_FILE
rm -f "$DEPLOYMENT_FILE.bak"

echo "Applying the namespace..."
kubectl apply -f kubernetes/namespace.yaml || { rollback_help; exit 1; }

echo "Creating the secret $APP_NAME-secrets..."
kubectl create secret generic $APP_NAME-secrets -n $NAMESPACE --from-literal=REDIS_PASSWORD="$REDIS_PASSWORD" --dry-run=client -o yaml | kubectl apply -f - || { rollback_help; exit 1; }

echo "Applying the deployment, the service and the ingress..."
kubectl apply -f kubernetes/deployment.yaml || { rollback_help; exit 1; }
kubectl apply -f kubernetes/service.yaml || { rollback_help; exit 1; }
kubectl apply -f kubernetes/ingress.yaml || { rollback_help; exit 1; }

echo "Waiting for the rollout..."
kubectl rollout status deployment/$APP_NAME -n $NAMESPACE --timeout=300s || { rollback_help; exit 1; }

echo "Deployment state:"
kubectl get pods,svc,ingress -n $NAMESPACE

APP_HOST=$(kubectl get ingress $APP_NAME-ingress -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || true)
if [ -n "$APP_HOST" ]; then
  echo "Application URL: http://$APP_HOST"
else
  echo "The load balancer address is not ready yet. Check later with: kubectl get ingress -n $NAMESPACE"
fi
echo "To roll back: kubectl rollout undo deployment/$APP_NAME -n $NAMESPACE"
