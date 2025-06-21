#!/bin/bash

# 检查是否开启debug模式
DEBUG_MODE=false
if [[ "$1" == "--debug" ]]; then
    DEBUG_MODE=true
    echo "Debug模式已开启"
fi

echo "开始部署..."

# 步骤1: Maven 打包
echo "步骤1: 执行 Maven 打包..."
mvn clean package -DskipTests

# 步骤2: 查找并停止现有的Java进程
echo "步骤2: 查找并停止现有的Java进程..."
JAVA_PID=$(ps aux | grep '[j]ava' | grep 'taichubackend-starter-1.0-SNAPSHOT.jar' | awk '{print $2}')

if [ -n "$JAVA_PID" ]; then
    echo "找到Java进程 PID: $JAVA_PID，正在停止..."
    kill $JAVA_PID
    sleep 3
else
    echo "未找到运行中的Java进程"
fi

# 步骤3: 切换到目标目录
echo "步骤3: 切换到目标目录..."
cd /root/taichu-fic-workspace/taichufic/taichubackend-starter/target

# 步骤4: 启动新的Java进程
echo "步骤4: 启动新的Java进程..."
if [ "$DEBUG_MODE" = true ]; then
    echo "使用debug模式启动..."
    nohup java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:9002 \
        -Dspring.profiles.active=dev \
        -jar taichubackend-starter-1.0-SNAPSHOT.jar \
        > app.log 2>&1 &
else
    echo "使用普通模式启动..."
    nohup java -Dspring.profiles.active=dev \
        -jar taichubackend-starter-1.0-SNAPSHOT.jar \
        > app.log 2>&1 &
fi

echo "Java进程已启动"

# 步骤5: 显示启动日志
echo "步骤5: 显示启动日志 (按 Ctrl+C 退出)"
tail -f app.log 