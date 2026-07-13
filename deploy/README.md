# 部署目录

本目录提供 openEuler/Linux 目标环境的 Nginx、systemd 和环境变量模板。真实密码只写入服务器上的 `/etc/medicine/medicine-backend.env`，权限设置为 `0600`，不要提交到 Git。

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

