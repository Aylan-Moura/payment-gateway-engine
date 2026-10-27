#!/usr/bin/env bash
#
# Backup logico do PostgreSQL do Payment Gateway Engine.
#
# Uso:
#   bash scripts/backup.sh
#
# Variaveis (todas opcionais):
#   COMPOSE_FILE           compose usado (default: docker-compose.prod.yml)
#   POSTGRES_SERVICE       servico do postgres no compose (default: postgres)
#   POSTGRES_USER          usuario do banco (default: admin)
#   POSTGRES_DB            banco (default: payment_gateway)
#   BACKUP_DIR             diretorio dos dumps (default: /var/backups/payment-gateway)
#   BACKUP_RETENTION_DAYS  dias de retencao (default: 7)
#
# O dump e comprimido, verificado com gzip -t e acompanhado de um .sha256.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"
POSTGRES_SERVICE="${POSTGRES_SERVICE:-postgres}"
POSTGRES_USER="${POSTGRES_USER:-admin}"
POSTGRES_DB="${POSTGRES_DB:-payment_gateway}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/payment-gateway}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"

if [ ! -f "$PROJECT_DIR/$COMPOSE_FILE" ]; then
  echo "Falha: arquivo de compose nao encontrado: $PROJECT_DIR/$COMPOSE_FILE" >&2
  exit 1
fi

if ! [[ "$BACKUP_RETENTION_DAYS" =~ ^[0-9]+$ ]]; then
  echo "Falha: BACKUP_RETENTION_DAYS deve ser um inteiro (recebido: $BACKUP_RETENTION_DAYS)" >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR"

TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
BASENAME="payment_gateway_${TIMESTAMP}"
TARGET="$BACKUP_DIR/${BASENAME}.sql.gz"
PARTIAL="${TARGET}.part"

echo "==> Gerando dump de $POSTGRES_DB em $TARGET"

cleanup() {
  rm -f "$PARTIAL"
}
trap cleanup EXIT

cd "$PROJECT_DIR"
docker compose -f "$COMPOSE_FILE" exec -T "$POSTGRES_SERVICE" \
  pg_dump --no-owner --no-privileges -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  | gzip -9 > "$PARTIAL"

# Nao publica um dump truncado: valida antes de renomear.
gzip -t "$PARTIAL"

mv "$PARTIAL" "$TARGET"
sha256sum "$TARGET" > "${TARGET}.sha256"

# Retencao: remove dumps (e checksums) mais antigos que N dias.
find "$BACKUP_DIR" -maxdepth 1 -type f -name 'payment_gateway_*.sql.gz' \
  -mtime +"$BACKUP_RETENTION_DAYS" -print -delete
find "$BACKUP_DIR" -maxdepth 1 -type f -name 'payment_gateway_*.sql.gz.sha256' \
  -mtime +"$BACKUP_RETENTION_DAYS" -print -delete

echo "==> Backup concluido: $TARGET ($(du -h "$TARGET" | cut -f1))"
echo "==> Retencao local: ${BACKUP_RETENTION_DAYS} dias"
