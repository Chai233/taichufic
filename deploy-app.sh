#!/bin/bash

# 设置错误时退出
set -e

echo "开始应用部署流程..."

# 1. 检查 Docker 网络是否存在
echo "检查 Docker 网络状态..."
if ! docker network ls | grep taichu_network &> /dev/null; then
    echo "错误：taichu_network 网络不存在，请先运行 deploy-mysql.sh"
    exit 1
fi

# 2. 检查数据库是否运行
echo "检查数据库状态..."
if ! docker ps | grep mysql_demo &> /dev/null; then
    echo "错误：数据库未运行，请先运行 deploy-mysql.sh"
    exit 1
fi

# 3. 创建日志目录并设置权限
echo "创建日志目录..."
mkdir -p logs
chmod 777 logs

# 4. 启动应用服务
echo "启动应用服务..."
docker-compose -f docker-compose.app.yml up -d --build

# 5. 检查应用服务状态
echo "检查应用服务状态..."
if ! docker ps | grep taichu_app &> /dev/null; then
    echo "错误：应用服务未成功启动"
    exit 1
fi

# 6. 等待应用启动
echo "等待应用启动..."
sleep 30

# 7. 检查应用健康状态
echo "检查应用健康状态..."
if ! curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
    echo "错误：应用服务未正常运行"
    exit 1
fi

# 8. 检查日志文件是否创建
echo "检查日志文件..."
if [ ! -f logs/application.log ]; then
    echo "警告：应用日志文件未创建，请检查应用日志配置"
fi

echo "应用部署完成！"
echo "日志文件位置：./logs/"
echo "- application.log：应用日志"
echo "- error.log：错误日志" 