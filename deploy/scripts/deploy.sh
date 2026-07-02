#!/bin/bash

set -e

DEPLOY_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
REPO_DIR=$(cd "$DEPLOY_DIR/.." && pwd)
ENVIRONMENT="${1:-staging}"
TAG="${2:-latest}"
COMPOSE_FILE="docker-compose.${ENVIRONMENT}.yml"
BACKUP_DIR="$DEPLOY_DIR/backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

log() {
    echo "[$(date +"%Y-%m-%d %H:%M:%S")] [$ENVIRONMENT] $1"
}

error() {
    log "ERROR: $1" >&2
    exit 1
}

validate_environment() {
    if [[ ! -f "$DEPLOY_DIR/$COMPOSE_FILE" ]]; then
        error "Compose file not found: $COMPOSE_FILE"
    fi
    if [[ ! "$ENVIRONMENT" =~ ^(development|staging|production)$ ]]; then
        error "Invalid environment: $ENVIRONMENT. Must be development, staging, or production"
    fi
}

backup_configs() {
    log "Creating backup of current configurations..."
    mkdir -p "$BACKUP_DIR/$TIMESTAMP"
    cp "$DEPLOY_DIR/$COMPOSE_FILE" "$BACKUP_DIR/$TIMESTAMP/"
    cp "$REPO_DIR/.env" "$BACKUP_DIR/$TIMESTAMP/" 2>/dev/null || true
    log "Backup created at: $BACKUP_DIR/$TIMESTAMP"
}

pull_latest_code() {
    log "Pulling latest code from repository..."
    cd "$REPO_DIR"
    git pull origin "$(git branch --show-current)"
    cd "$DEPLOY_DIR"
}

build_images() {
    log "Building Docker images with tag: $TAG..."
    cd "$REPO_DIR"
    docker compose -f "$DEPLOY_DIR/$COMPOSE_FILE" build
    cd "$DEPLOY_DIR"
}

deploy_services() {
    log "Deploying services..."
    cd "$DEPLOY_DIR"
    docker compose -f "$COMPOSE_FILE" up -d --remove-orphans
}

health_check() {
    log "Performing health checks..."
    local services=("core-gateway" "lite-gateway" "user-service")
    local all_healthy=true

    for service in "${services[@]}"; do
        local max_attempts=10
        local attempt=1
        local healthy=false

        while [[ $attempt -le $max_attempts ]]; do
            log "Checking $service health (attempt $attempt/$max_attempts)..."
            if docker compose -f "$COMPOSE_FILE" exec "$service" curl -f -s "http://localhost:8080/actuator/health" >/dev/null 2>&1 || \
               docker inspect -f '{{.State.Health.Status}}' "tailor-is-${service}-${ENVIRONMENT}" 2>/dev/null | grep -q healthy; then
                healthy=true
                break
            fi
            attempt=$((attempt + 1))
            sleep 10
        done

        if [[ $healthy == false ]]; then
            log "WARNING: $service health check failed"
            all_healthy=false
        else
            log "OK: $service is healthy"
        fi
    done

    if [[ $all_healthy == false ]]; then
        log "WARNING: Some services failed health check"
        return 1
    fi
    return 0
}

rollback() {
    local backup_time="${1:-latest}"
    
    if [[ "$backup_time" == "latest" ]]; then
        backup_time=$(ls -dt "$BACKUP_DIR"/*/ 2>/dev/null | head -n 1 | xargs basename)
    fi

    if [[ -z "$backup_time" ]]; then
        error "No backup found to rollback to"
    fi

    log "Rolling back to backup: $backup_time..."
    cd "$DEPLOY_DIR"
    
    cp "$BACKUP_DIR/$backup_time/$COMPOSE_FILE" "$DEPLOY_DIR/$COMPOSE_FILE"
    cp "$BACKUP_DIR/$backup_time/.env" "$REPO_DIR/.env" 2>/dev/null || true
    
    docker compose -f "$COMPOSE_FILE" down
    docker compose -f "$COMPOSE_FILE" up -d
    
    log "Rollback completed"
}

show_help() {
    echo "Usage: $0 <environment> [tag]"
    echo "Environment: development, staging, production"
    echo "Tag: Docker image tag (default: latest)"
    echo ""
    echo "Commands:"
    echo "  $0 deploy <environment> [tag]  - Deploy to specified environment"
    echo "  $0 rollback <environment> [backup_time] - Rollback to backup"
    echo "  $0 status <environment> - Show service status"
    echo "  $0 backup <environment> - Create manual backup"
}

show_status() {
    log "Showing service status for $ENVIRONMENT..."
    cd "$DEPLOY_DIR"
    docker compose -f "$COMPOSE_FILE" ps
}

main() {
    local command="${1:-deploy}"
    
    case "$command" in
        deploy)
            ENVIRONMENT="${2:-staging}"
            TAG="${3:-latest}"
            validate_environment
            backup_configs
            pull_latest_code
            build_images
            deploy_services
            health_check || log "Health check warnings, but deployment completed"
            show_status
            log "Deployment completed successfully"
            ;;
        rollback)
            ENVIRONMENT="${2:-staging}"
            rollback "$3"
            show_status
            ;;
        status)
            ENVIRONMENT="${2:-staging}"
            validate_environment
            show_status
            ;;
        backup)
            ENVIRONMENT="${2:-staging}"
            backup_configs
            ;;
        *)
            show_help
            exit 1
            ;;
    esac
}

main "$@"