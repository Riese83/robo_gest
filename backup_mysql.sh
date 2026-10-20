#!/bin/bash
# ==============================================================================
# SCRIPT DE COPIA DE SEGURIDAD AUTOMATIZADA PARA ROBO_GEST (MYSQL EN DOCKER)
# Facultad Politecnica - Universidad Nacional del Este (FP-UNE)
# ==============================================================================

# Directorio de destino de los backups (creado si no existe)
BACKUP_DIR="${HOME}/Robo_Gest/backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
FILENAME="backup_robogest_${TIMESTAMP}.sql.gz"
CONTAINER_NAME="mysql_club_robotica"
DB_NAME="club_robotica"
DB_USER="root"
DB_PASS="root_password"

mkdir -p "${BACKUP_DIR}"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Iniciando respaldo de base de datos '${DB_NAME}'..."

# Ejecutar mysqldump dentro del contenedor y comprimir con gzip al vuelo
docker exec "${CONTAINER_NAME}" mysqldump -u "${DB_USER}" -p"${DB_PASS}" --single-transaction --routines --triggers "${DB_NAME}" | gzip > "${BACKUP_DIR}/${FILENAME}"

if [ $? -eq 0 ] && [ -s "${BACKUP_DIR}/${FILENAME}" ]; then
    TAMANHO=$(du -h "${BACKUP_DIR}/${FILENAME}" | cut -f1)
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Respaldo completado con exito: ${FILENAME} (${TAMANHO})"
else
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: Fallo al crear el respaldo de la base de datos." >&2
    exit 1
fi

# Rotacion automatica: Mantener los respaldos de los ultimos 30 dias y eliminar los mas antiguos
find "${BACKUP_DIR}" -type f -name "backup_robogest_*.sql.gz" -mtime +30 -exec rm -f {} \;
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Politica de retencion aplicada (se conservan ultimos 30 dias)."
