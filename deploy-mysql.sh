#!/bin/bash

# 设置错误时退出
set -e

echo "开始数据库部署流程..."

# 1. 检查并安装 Docker
echo "检查 Docker 安装状态..."
if ! command -v docker &> /dev/null; then
    echo "Docker 未安装，开始安装..."
    curl -fsSL https://get.docker.com | sh
    systemctl start docker
    systemctl enable docker
else
    echo "Docker 已安装"
fi

# 2. 检查并安装 Docker Compose
echo "检查 Docker Compose 安装状态..."
if ! command -v docker-compose &> /dev/null; then
    echo "Docker Compose 未安装，开始安装..."
    curl -L "https://github.com/docker/compose/releases/download/v2.24.1/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
else
    echo "Docker Compose 已安装"
fi

# 3. 启动数据库服务
echo "启动数据库服务..."
docker-compose -f docker-compose.mysql.yml up -d

# 4. 等待数据库启动
echo "等待数据库启动..."
sleep 10

# 5. 检查数据库容器状态
echo "检查数据库容器状态..."
if ! docker ps | grep mysql_demo &> /dev/null; then
    echo "错误：数据库容器未成功启动"
    exit 1
fi

# 6. 等待数据库完全就绪
echo "等待数据库完全就绪..."
for i in {1..30}; do
    if docker exec mysql_demo mysqladmin ping -h localhost -u root -proot123 &> /dev/null; then
        echo "数据库已就绪"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "错误：数据库未能在预期时间内就绪"
        exit 1
    fi
    sleep 2
done

# 7. 执行数据库初始化脚本
echo "执行数据库初始化脚本..."
docker exec -i mysql_demo mysql -uroot -proot123 demo_db_taichu < taichubackend-starter/src/main/resources/db/migration/V1_create_table.sql

# 8. 验证表是否创建成功
echo "验证数据库表创建状态..."
if ! docker exec mysql_demo mysql -uroot -proot123 demo_db_taichu -e "SHOW TABLES;" | grep -q "fic_workflow"; then
    echo "错误：数据库表创建失败"
    exit 1
fi

echo "数据库部署完成！" 