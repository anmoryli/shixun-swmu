# 部署目录

本目录提供 openEuler/Linux 目标环境的 Nginx、systemd 和环境变量模板。真实密码只写入服务器上的 `/etc/medicine/medicine-backend.env`，权限设置为 `0600`，不要提交到 Git。

## 推荐：Docker Compose

Docker 部署已经完成实际构建与回归验证。在仓库根目录执行：

```powershell
.\deploy\docker\init-secrets.ps1  # 首次配置密码时执行
docker compose up -d --build
```

密码配置、验证、日志、停止和升级方式见 `docker/README.md`。

## 产物

- 后端：`medical-backend/target/medical-backend-1.0.0.jar`
- 前端：`medical-managerment-system/dist/`

## 推荐目录

```text
/opt/medicine/app/medical-backend-1.0.0.jar
/opt/medicine/web/
/opt/medicine/uploads/
/etc/medicine/medicine-backend.env
/etc/systemd/system/medicine-backend.service
/etc/nginx/conf.d/medicine.conf
```

## 部署步骤

1. 创建非 root 用户和目录，并把上传目录授权给该用户。
2. 复制 JAR 和前端 `dist`。
3. 根据 `env/medicine-backend.env.example` 创建服务器私有配置。
4. 安装 systemd unit 与 Nginx 配置。
5. 执行 `systemctl daemon-reload && systemctl enable --now medicine-backend`。
6. 执行 `nginx -t` 后平滑重载 Nginx。
7. 检查 `/actuator/health`、登录、权限和各业务 GET。

完整命令、验证证据和回滚方法见 `process-docs/05-build-and-deployment.md`。

## 数据库备份

`scripts/backup-mysql.sh` 对 `medicine` 库做 `--single-transaction` 一致性逻辑备份,产出带时间戳的 gzip 并按保留天数轮转。数据库密码从 0600 权限的私有 env 文件读取,写入临时 `.my.cnf`,不落命令行参数(避免 `ps`/历史泄露)。

```bash
# 手动执行一次(读取 /etc/medicine/medicine-backend.env)
sudo deploy/scripts/backup-mysql.sh

# 自定义保留天数与目录
sudo KEEP_DAYS=14 BACKUP_DIR=/data/backup deploy/scripts/backup-mysql.sh
```

推荐用 systemd timer 每日凌晨备份(比 cron 更易观测与重试):

```ini
# /etc/systemd/system/medicine-backup.service
[Service]
Type=oneshot
ExecStart=/opt/medicine/app/deploy/scripts/backup-mysql.sh
User=root
```

```ini
# /etc/systemd/system/medicine-backup.timer
[Timer]
OnCalendar=*-*-* 03:30:00
Persistent=true
[Install]
WantedBy=timers.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now medicine-backup.timer
systemctl list-timers medicine-backup.timer   # 确认下次触发
journalctl -u medicine-backup.service         # 查看备份日志
```

备份文件落在 `/opt/medicine/backup/`(默认),保留 7 天。恢复示例:

```bash
gunzip < /opt/medicine/backup/medicine_20260715_033000.sql.gz | \
  mysql --defaults-file=/etc/medicine/my.cnf medicine
```

## Windows 本地私有环境变量

本机私有配置默认放在 `.work/private/medicine-backend.env.ps1`。`.work/` 已被 Git 忽略，真实密码不会进入版本库。`deploy/scripts/start-local.ps1` 会自动加载该文件；也可以通过 `-EnvFile` 指定其他私有文件。

```powershell
.\deploy\scripts\start-local.ps1
```
