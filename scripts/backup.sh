#!/bin/bash
# Запускать через cron на VPS:
# 0 3 * * * /opt/myapp/scripts/backup.sh

set -e

DB_PATH="/var/lib/myapp/data/app.db"
BACKUP_DIR="/var/backups/myapp"
DATE=$(date +%Y%m%d_%H%M)

mkdir -p $BACKUP_DIR
sqlite3 $DB_PATH ".backup $BACKUP_DIR/app_$DATE.db"
find $BACKUP_DIR -name "*.db" -mtime +7 -delete

echo "Backup done: $BACKUP_DIR/app_$DATE.db"
