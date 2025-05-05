#!/bin/bash

echo "📦 [1/4] 创建 docker-compose.yml 文件..."

cat > docker-compose.yml <<EOF
version: '3.8'
services:
  mysql:
    image: mysql:8
    container_name: mysql_demo
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: demo_db
      MYSQL_USER: demo
      MYSQL_PASSWORD: demo123
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

volumes:
  mysql_data:
EOF

echo "✅ docker-compose.yml 创建成功！"

echo "🚀 [2/4] 启动 MySQL 容器中..."

docker-compose up -d

echo "✅ MySQL 已启动。等待容器准备好（可能需要几秒）..."
sleep 10

echo "🔍 [3/4] 测试数据库连接..."

docker exec -it mysql_demo mysql -udemo -pdemo123 -e "SHOW DATABASES;" 2>/dev/null

if [ $? -eq 0 ]; then
  echo "✅ 数据库连接成功！MySQL 正常运行。"
else
  echo "❌ 数据库连接失败，请检查 Docker 和端口映射。"
  exit 1
fi

echo "📎 [4/4] 建议你现在修改 application.yml 数据源为："
echo ""
echo "spring:"
echo "  datasource:"
echo "    url: jdbc:mysql://localhost:3306/demo_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
echo "    username: demo"
echo "    password: demo123"
echo "  jpa:"
echo "    hibernate:"
echo "      ddl-auto: update"
echo "    show-sql: true"
echo "    database-platform: org.hibernate.dialect.MySQL8Dialect"
echo ""

echo "🎉 初始化完成！你现在可以运行 Spring Boot 项目了。"
