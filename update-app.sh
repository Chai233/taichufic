#!/bin/bash

# 设置错误时退出
set -e

echo "开始更新应用..."

# 1. 检查数据库是否运行
echo "检查数据库状态..."
if ! docker ps | grep mysql_demo &> /dev/null; then
    echo "错误：数据库未运行，请先运行 deploy-mysql.sh"
    exit 1
fi

# 2. 备份当前日志
echo "备份当前日志..."
if [ -d "~/logs/taichu-fic" ]; then
    backup_dir="~/logs/taichu-fic_backup_$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$backup_dir"
    mv ~/logs/taichu-fic/* "$backup_dir" 2>/dev/null || true
    echo "日志已备份到 $backup_dir"
fi

# 3. 重新创建日志目录
echo "创建新的日志目录..."
mkdir -p ~/logs/taichu-fic
chmod 777 ~/logs/taichu-fic

# 4. 停止并删除旧的应用容器
echo "停止旧的应用容器..."
docker-compose -f docker-compose.app.yml down

# 5. 清理旧的构建缓存
echo "清理构建缓存..."
docker builder prune -f

# 6. 重新构建并启动应用
echo "重新构建并启动应用..."
docker-compose -f docker-compose.app.yml up -d --build

# 7. 检查应用服务状态
echo "检查应用服务状态..."
if ! docker ps | grep taichu_app &> /dev/null; then
    echo "错误：应用服务未成功启动"
    exit 1
fi

# 8. 等待应用启动
echo "等待应用启动..."
sleep 30

# 9. 检查应用健康状态
echo "检查应用健康状态..."
if ! curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
    echo "错误：应用服务未正常运行"
    exit 1
fi

# 10. 检查日志文件是否创建
echo "检查日志文件..."
if [ ! -f ~/logs/taichu-fic/application.log ]; then
    echo "警告：应用日志文件未创建，请检查应用日志配置"
fi

echo "应用更新完成！"
echo "日志文件位置：~/logs/taichu-fic/"
echo "- application.log：应用日志"
echo "- error.log：错误日志"

# 11. 显示容器日志
echo "显示应用启动日志..."
docker-compose -f docker-compose.app.yml logs --tail=50 