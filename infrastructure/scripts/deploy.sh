#!/bin/bash
# SOSO Server - Production Deployment Script
# Usage: ./infrastructure/scripts/deploy.sh [environment]

set -euo pipefail

# =============================================================================
# Configuration
# =============================================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENVIRONMENT="${1:-production}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# =============================================================================
# Pre-deployment Checks
# =============================================================================
check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check if Docker is running
    if ! docker info >/dev/null 2>&1; then
        log_error "Docker is not running or not accessible"
        exit 1
    fi

    # Check if Docker Compose is available
    if ! command -v docker >/dev/null 2>&1; then
        log_error "Docker Compose is not installed"
        exit 1
    fi

    # Check if we're in the right directory
    if [[ ! -f "$PROJECT_ROOT/compose.yml" ]]; then
        log_error "compose.yml not found. Please run this script from the project root."
        exit 1
    fi

    # Check if environment file exists
    if [[ ! -f "$PROJECT_ROOT/.env" ]]; then
        log_error ".env file not found. Please copy .env.example to .env and configure it."
        exit 1
    fi

    log_success "Prerequisites check passed"
}

# =============================================================================
# Backup Functions
# =============================================================================
backup_database() {
    log_info "Creating database backup..."

    local backup_dir="$PROJECT_ROOT/backups/$(date +%Y%m%d)"
    mkdir -p "$backup_dir"

    if docker compose ps db | grep -q "running"; then
        docker compose exec -T db mysqldump \
            --single-transaction \
            --routines \
            --triggers \
            --all-databases \
            -u root -p"${MYSQL_ROOT_PASSWORD}" \
            > "$backup_dir/mysql_backup_$(date +%H%M%S).sql" 2>/dev/null || {
            log_warning "Database backup failed, but continuing..."
        }
        log_success "Database backup created: $backup_dir"
    else
        log_warning "Database not running, skipping backup"
    fi
}

# =============================================================================
# Deployment Functions
# =============================================================================
deploy_services() {
    log_info "Starting deployment process..."

    cd "$PROJECT_ROOT"

    # Load environment variables
    if [[ -f ".env" ]]; then
        set -a
        source .env
        set +a
    fi

    # Pull latest images
    log_info "Pulling latest base images..."
    docker compose pull db redis proxy || true

    # Start core services first
    log_info "Starting core services (DB, Redis)..."
    docker compose up -d db redis

    # Wait for dependencies
    log_info "Waiting for dependencies to be healthy..."
    local max_wait=120
    local wait_time=0

    while [[ $wait_time -lt $max_wait ]]; do
        if docker compose ps db | grep -q "healthy" && \
           docker compose ps redis | grep -q "healthy"; then
            log_success "Dependencies are healthy"
            break
        fi

        if [[ $wait_time -eq $max_wait ]]; then
            log_error "Dependencies failed to become healthy within ${max_wait}s"
            docker compose ps
            docker compose logs db redis
            exit 1
        fi

        echo -n "."
        sleep 5
        wait_time=$((wait_time + 5))
    done

    # Deploy API service
    log_info "Deploying API service..."
    docker compose up -d api

    # Wait for API to be healthy
    log_info "Waiting for API service to be healthy..."
    wait_time=0
    max_wait=180

    while [[ $wait_time -lt $max_wait ]]; do
        if docker compose exec -T api curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
            log_success "API service is healthy"
            break
        fi

        if [[ $wait_time -eq $max_wait ]]; then
            log_error "API service failed to become healthy within ${max_wait}s"
            docker compose ps api
            docker compose logs api
            exit 1
        fi

        echo -n "."
        sleep 10
        wait_time=$((wait_time + 10))
    done

    # Start proxy
    log_info "Starting reverse proxy..."
    docker compose up -d proxy

    # Final verification
    log_info "Final system verification..."
    docker compose ps

    log_success "Deployment completed successfully!"
}

# =============================================================================
# Cleanup Functions
# =============================================================================
cleanup_old_resources() {
    log_info "Cleaning up old resources..."

    # Remove old images (keep last 3 versions)
    docker images --format "table {{.Repository}}:{{.Tag}}\t{{.CreatedAt}}" | \
        grep "localtest/soso-server" | \
        tail -n +4 | \
        awk '{print $1}' | \
        xargs -r docker rmi || true

    # Clean up unused volumes and networks
    docker volume prune -f || true
    docker network prune -f || true

    log_success "Cleanup completed"
}

# =============================================================================
# Health Check
# =============================================================================
health_check() {
    log_info "Performing health check..."

    local services=("db" "redis" "api" "proxy")
    local all_healthy=true

    for service in "${services[@]}"; do
        if docker compose ps "$service" | grep -q "running"; then
            log_success "✅ $service is running"
        else
            log_error "❌ $service is not running"
            all_healthy=false
        fi
    done

    # Test external endpoints
    local endpoints=(
        "https://soso.dreampaste.com/actuator/health"
        "https://soso.dreampaste.com/swagger-ui/"
        "https://soso.dreampaste.com/jenkins/"
    )

    for endpoint in "${endpoints[@]}"; do
        if curl -f -s "$endpoint" >/dev/null 2>&1; then
            log_success "✅ $endpoint is accessible"
        else
            log_warning "⚠️ $endpoint is not accessible"
        fi
    done

    if [[ "$all_healthy" == true ]]; then
        log_success "🎉 All services are healthy!"
    else
        log_error "❌ Some services are not healthy"
        return 1
    fi
}

# =============================================================================
# Main Execution
# =============================================================================
main() {
    echo "🚀 SOSO Server Deployment Script"
    echo "Environment: $ENVIRONMENT"
    echo "Project Root: $PROJECT_ROOT"
    echo "================================"

    check_prerequisites
    backup_database
    deploy_services
    cleanup_old_resources
    health_check

    echo ""
    echo "🎉 Deployment completed successfully!"
    echo "🌐 Service URLs:"
    echo "   • Main Site: https://soso.dreampaste.com"
    echo "   • API Docs: https://soso.dreampaste.com/swagger-ui/"
    echo "   • Jenkins: https://soso.dreampaste.com/jenkins/"
    echo ""
}

# Handle script interruption
trap 'log_error "Script interrupted"; exit 1' INT TERM

# Execute main function
main "$@"