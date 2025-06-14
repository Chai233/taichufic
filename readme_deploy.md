# 部署说明文档

## 一、环境要求

- Linux 服务器
- Docker
- Docker Compose
- Docker源改为阿里云
- 开放端口：
  - 8080：应用服务
  - 3306：数据库服务

### HowTo: Docker源改为阿里云
切换镜像源为阿里云，参考 https://cr.console.aliyun.com/cn-hangzhou/instances/mirrors
```shell
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": ["https://yf8jt11h.mirror.aliyuncs.com"]
}
EOF
sudo systemctl daemon-reload
sudo systemctl restart docker
```

## 二、首次部署

### 1. 准备工作

```bash
# 克隆项目代码
git clone <项目仓库地址>
cd taichubackend

# 给部署脚本添加执行权限
chmod +x deploy-mysql.sh deploy-app.sh
```

### 2. 部署数据库

```bash
# 运行数据库部署脚本
./deploy-mysql.sh
```

该脚本会：
- 检查并安装 Docker 和 Docker Compose
- 启动 MySQL 容器
- 初始化数据库
- 创建必要的表

### 3. 部署应用

```bash
# 运行应用部署脚本
./deploy-app.sh
```

该脚本会：
- 检查数据库是否正常运行
- 创建日志目录
- 构建并启动应用容器
- 检查应用健康状态

### 4. 验证部署

```bash
# 检查容器状态
docker ps

# 查看应用日志
tail -f ~/logs/taichu-fic/application.log

# 查看错误日志
tail -f ~/logs/taichu-fic/error.log

# 测试应用健康状态
curl http://localhost:8080/actuator/health
```

## 三、更新部署

### 1. 准备工作

```bash
# 更新代码
git pull

# 给更新脚本添加执行权限
chmod +x update-app.sh
```

### 2. 执行更新

```bash
# 运行更新脚本
./update-app.sh
```

该脚本会：
- 检查数据库是否运行
- 备份当前日志
- 停止并删除旧的应用容器
- 清理构建缓存
- 重新构建并启动应用
- 验证应用健康状态

## 四、日志管理

### 1. 日志位置

所有日志文件存储在 `~/logs/taichu-fic/` 目录下：
```
~/logs/taichu-fic/
├── application.log                # 当前应用日志
├── application.2024-01-20.0.log  # 按日期和大小分割的日志
├── error.log                     # 当前错误日志
└── error.2024-01-20.0.log        # 按日期和大小分割的错误日志
```

### 2. 查看日志

```bash
# 查看应用日志
tail -f ~/logs/taichu-fic/application.log

# 查看错误日志
tail -f ~/logs/taichu-fic/error.log

# 查看容器日志
docker-compose -f docker-compose.app.yml logs -f
```

## 五、常用维护命令

### 1. 容器管理

```bash
# 查看所有容器状态
docker ps

# 重启数据库
docker-compose -f docker-compose.mysql.yml restart

# 重启应用
docker-compose -f docker-compose.app.yml restart

# 停止数据库
docker-compose -f docker-compose.mysql.yml down

# 停止应用
docker-compose -f docker-compose.app.yml down
```

### 2. 日志管理

```bash
# 查看数据库日志
docker-compose -f docker-compose.mysql.yml logs -f

# 查看应用日志
docker-compose -f docker-compose.app.yml logs -f

# 查看特定时间段的日志
docker-compose -f docker-compose.app.yml logs --since 2024-01-01T00:00:00
```

## 六、故障排查

1. **数据库连接问题**
   - 检查数据库容器是否运行：`docker ps | grep mysql_demo`
   - 检查数据库日志：`docker-compose -f docker-compose.mysql.yml logs`

2. **应用启动问题**
   - 检查应用容器是否运行：`docker ps | grep taichu_app`
   - 检查应用日志：`tail -f ~/logs/taichu-fic/application.log`
   - 检查错误日志：`tail -f ~/logs/taichu-fic/error.log`

3. **网络问题**
   - 检查网络连接：`docker network inspect taichu_network`
   - 检查端口占用：`netstat -tulpn | grep 8080`

## 七、注意事项

1. 部署前确保服务器有足够的磁盘空间
2. 确保防火墙已开放必要端口
3. 数据库数据会持久化在 Docker volume 中
4. 应用日志会自动按日期和大小分割
5. 更新时会自动备份旧日志
