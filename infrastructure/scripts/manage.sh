#!/bin/bash
# SOSO Server - Management Utilities Script
# Usage: ./infrastructure/scripts/manage.sh [command] [options]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

cd "$PROJECT_ROOT"

# =============================================================================
# Management Commands
# =============================================================================

show_status() {
    log_info "SOSO Server Status"
    echo "===================="

    echo -e "\n📋 Container Status:"
    docker compose ps

    echo -e "\n💾 Resource Usage:"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}"

    echo -e "\n🌐 Network Status:"
    docker network ls | grep soso || echo "No SOSO networks found"

    echo -e "\n💿 Volume Usage:"
    docker system df
}

show_logs() {
    local service="${2:-}"
    local lines="${3:-50}"

    if [[ -n "$service" ]]; then
        log_info "Showing logs for $service (last $lines lines)"
        docker compose logs --tail "$lines" "$service"
    else
        log_info "Showing logs for all services (last $lines lines)"
        docker compose logs --tail "$lines"
    fi
}

backup_data() {
    local backup_dir="$PROJECT_ROOT/backups/$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$backup_dir"

    log_info "Creating backup in $backup_dir"

    # Database backup
    if docker compose ps db | grep -q "running"; then
        log_info "Backing up database..."
        docker compose exec -T db mysqldump \
            --single-transaction \
            --routines \
            --triggers \
            --all-databases \
            -u root -p"${MYSQL_ROOT_PASSWORD:-}" \
            > "$backup_dir/database.sql" 2>/dev/null || {
            log_error "Database backup failed"
            return 1
        }
    fi

    # Redis backup
    if docker compose ps redis | grep -q "running"; then
        log_info "Backing up Redis..."
        docker compose exec -T redis redis-cli --rdb - > "$backup_dir/redis.rdb" || {
            log_warning "Redis backup failed"
        }
    fi

    # Configuration backup
    log_info "Backing up configuration..."
    cp -r infrastructure "$backup_dir/"
    cp .env "$backup_dir/.env.backup" 2>/dev/null || true
    cp compose.yml "$backup_dir/"

    log_success "Backup completed: $backup_dir"
}

restart_service() {
    local service="${2:-}"

    if [[ -z "$service" ]]; then
        log_error "Please specify a service to restart"
        echo "Available services: api, db, redis, proxy, jenkins"
        return 1
    fi

    log_info "Restarting service: $service"
    docker compose restart "$service"

    # Wait for service to be healthy
    sleep 5
    docker compose ps "$service"
}

update_service() {
    local service="${2:-api}"

    log_info "Updating service: $service"

    case "$service" in
        "api")
            log_info "Stopping API service..."
            docker compose stop api
            docker compose rm -f api

            log_info "Pulling latest image..."
            docker compose pull api || true

            log_info "Starting API service..."
            docker compose up -d api
            ;;
        "all")
            log_info "Updating all services..."
            docker compose pull
            docker compose up -d
            ;;
        *)
            log_info "Updating $service..."
            docker compose stop "$service"
            docker compose pull "$service" || true
            docker compose up -d "$service"
            ;;
    esac

    log_success "Update completed for $service"
}

cleanup_system() {
    log_info "Cleaning up Docker system..."

    # Remove stopped containers
    docker container prune -f

    # Remove unused images
    docker image prune -f

    # Remove unused volumes
    docker volume prune -f

    # Remove unused networks
    docker network prune -f

    # Remove build cache
    docker builder prune -f

    log_success "System cleanup completed"
}

monitor_services() {
    local duration="${2:-300}"

    log_info "Monitoring services for ${duration} seconds..."
    log_info "Press Ctrl+C to stop monitoring"

    trap 'log_info "Monitoring stopped"; exit 0' INT

    local end_time=$(($(date +%s) + duration))

    while [[ $(date +%s) -lt $end_time ]]; do
        clear
        echo "🔍 SOSO Server Monitoring - $(date)"
        echo "=================================="

        echo -e "\n📋 Service Status:"
        docker compose ps

        echo -e "\n💾 Resource Usage:"
        docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}"

        echo -e "\n🏥 Health Status:"
        for service in api db redis proxy; do
            if docker compose exec -T "$service" echo "alive" >/dev/null 2>&1; then
                echo "✅ $service: Healthy"
            else
                echo "❌ $service: Unhealthy"
            fi
        done

        sleep 10
    done
}

show_help() {
    echo "SOSO Server Management Script"
    echo "============================="
    echo ""
    echo "Usage: $0 [command] [options]"
    echo ""
    echo "Commands:"
    echo "  status                    Show system status"
    echo "  logs [service] [lines]    Show logs (default: all services, 50 lines)"
    echo "  backup                    Create full system backup"
    echo "  restart [service]         Restart a specific service"
    echo "  update [service]          Update a service (default: api)"
    echo "  cleanup                   Clean up unused Docker resources"
    echo "  monitor [duration]        Monitor services (default: 300 seconds)"
    echo "  help                      Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 status"
    echo "  $0 logs api 100"
    echo "  $0 restart api"
    echo "  $0 update all"
    echo "  $0 monitor 600"
}

# =============================================================================
# Main Execution
# =============================================================================

case "${1:-help}" in
    "status")
        show_status
        ;;
    "logs")
        show_logs "$@"
        ;;
    "backup")
        backup_data
        ;;
    "restart")
        restart_service "$@"
        ;;
    "update")
        update_service "$@"
        ;;
    "cleanup")
        cleanup_system
        ;;
    "monitor")
        monitor_services "$@"
        ;;
    "help"|*)
        show_help
        ;;
esac