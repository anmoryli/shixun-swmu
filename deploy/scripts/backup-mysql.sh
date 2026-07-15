#!/usr/bin/env bash
# MySQL 逻辑备份脚本:对 medicine 库做 --single-transaction 一致性快照,
# 产出带时间戳的 gzip 文件并按保留天数轮转。适用于 openEuler/Linux 部署机。
#
# 用法:
#   sudo deploy/scripts/backup-mysql.sh                 # 读取 /etc/medicine/medicine-backend.env
#   sudo ENV_FILE=/path/env deploy/scripts/backup-mysql.sh
#   sudo KEEP_DAYS=14 BACKUP_DIR=/data/backup deploy/scripts/backup-mysql.sh
#
# 安全:密码来自 0600 权限的私有 env 文件,不落命令行参数(避免 ps/历史泄露)。
# 退出码:0 成功;非 0 表示备份失败,可被 cron/systemd 捕获告警。
set -euo pipefail

ENV_FILE="${ENV_FILE:-/etc/medicine/medicine-backend.env}"
BACKUP_DIR="${BACKUP_DIR:-/opt/medicine/backup}"
KEEP_DAYS="${KEEP_DAYS:-7}"
# 使用 .my.cnf 传递密码,避免 --password 暴露在进程列表
MY_CNF="$(mktemp)"
trap 'rm -f "$MY_CNF"' EXIT

if [ -r "$ENV_FILE" ]; then
  # shellcheck disable=SC1090
  set -a; . "$ENV_FILE"; set +a
else
  echo "[backup-mysql] 警告:未找到 env 文件 $ENV_FILE,将依赖已导出的环境变量" >&2
fi

# 从 JDBC URL 解析 host/port/schema: jdbc:mysql://host:port/schema?...
DB_URL="${DB_URL:-}"
MYSQL_HOST="${MYSQL_HOST:-}"
MYSQL_PORT="${MYSQL_PORT:-}"
MYSQL_DB="${MYSQL_DB:-}"
if [ -n "$DB_URL" ]; then
  rest="${DB_URL#jdbc:mysql://}"
  auth="${rest%%/*}"          # host:port,(host:port,...)
  schema_part="${rest#*/}"    # schema?params
  schema="${schema_part%%\?*}"
  [ -z "$MYSQL_HOST" ] && MYSQL_HOST="${auth%%:*}"
  [ -z "$MYSQL_PORT" ] && [[ "$auth" == *:* ]] && MYSQL_PORT="${auth##*:}"
  [ -z "$MYSQL_DB" ] && MYSQL_DB="$schema"
fi
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DB="${MYSQL_DB:-medicine}"
MYSQL_USER="${DB_USERNAME:-${MYSQL_USER:-medicine_app}}"
MYSQL_PASSWORD="${DB_PASSWORD:-${MYSQL_PASSWORD:-}}"

if [ -z "$MYSQL_PASSWORD" ]; then
  echo "[backup-mysql] 错误:未提供 DB_PASSWORD,终止备份" >&2
  exit 2
fi

# 写入临时 .my.cnf,权限 0600
umask 077
printf '[client]\nuser=%s\npassword=%s\nhost=%s\nport=%s\n' \
  "$MYSQL_USER" "$MYSQL_PASSWORD" "$MYSQL_HOST" "$MYSQL_PORT" > "$MY_CNF"

mkdir -p "$BACKUP_DIR"
ts="$(date +%Y%m%d_%H%M%S)"
out="$BACKUP_DIR/${MYSQL_DB}_${ts}.sql.gz"

echo "[backup-mysql] 开始备份 $MYSQL_DB@$MYSQL_HOST:$MYSQL_PORT -> $out"
# --single-transaction: InnoDB 一致性快照,不锁表;--routines/--triggers/--events:含存储过程触发器事件;
# --set-gtid-purged=OFF:避免恢复时 GTID 冲突;--no-tablespaces:避免 PROCESS 权限问题。
mysqldump --defaults-file="$MY_CNF" \
  --single-transaction --quick --routines --triggers --events \
  --set-gtid-purged=OFF --no-tablespaces \
  "$MYSQL_DB" | gzip > "$out"

size="$(stat -c%s "$out" 2>/dev/null || stat -f%z "$out")"
echo "[backup-mysql] 备份完成: $out ($(awk "BEGIN{printf \"%.1f KB\", $size/1024}"))"

# 按保留天数清理旧备份
find "$BACKUP_DIR" -maxdepth 1 -name "${MYSQL_DB}_*.sql.gz" -type f -mtime "+${KEEP_DAYS}" -delete
echo "[backup-mysql] 已清理 ${KEEP_DAYS} 天前的旧备份"
