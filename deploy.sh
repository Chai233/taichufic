#!/bin/bash

# 帮助函数
function print_help() {
    echo "用法: ./deploy.sh [选项]"
    echo "部署太初后端应用。"
    echo ""
    echo "选项:"
    echo "  --debug                     开启Java远程调试，监听 9002 端口。"
    echo "  --mockAlgo                  使用 AlgoGateway 的 mock 实现。"
    echo "  --algo-small-scale-test     将 'algo.small-scale-test' 属性设置为 true。"
    echo "  --use-80-port               将应用端口设置为 80。"
    echo "  -h, --help                  显示此帮助信息并退出。"
}

# 解析命令行参数
DEBUG_MODE=false
MOCK_ALGO=false
ALGO_SMALL_SCALE_TEST=false
USE_80_PORT=false
for arg in "$@"
do
    case $arg in
        -h|--help)
        print_help
        exit 0
        ;;
        --debug)
        DEBUG_MODE=true
        ;;
        --mockAlgo)
        MOCK_ALGO=true
        ;;
        --algo-small-scale-test)
        ALGO_SMALL_SCALE_TEST=true
        ;;
        --use-80-port)
        USE_80_PORT=true
        ;;
    esac
done

if [ "$DEBUG_MODE" = true ]; then
    echo "Debug模式已开启"
fi

if [ "$MOCK_ALGO" = true ]; then
    echo "Algo Mock模式已开启"
    export ALGO_SERVICE_MOCK=true
else
    echo "Algo Mock模式未开启，将使用真实服务"
    echo "请确保已手动连接VPN并可以访问目标服务"
    export ALGO_SERVICE_MOCK=false
    
    # 检查VPN连接状态
    echo "检查VPN连接状态..."
    echo "正在ping目标IP: 192.168.100.106"
    
    if ping -c 3 192.168.100.106 >/dev/null 2>&1; then
        echo "✓ VPN连接正常，目标IP可达"
    else
        echo "✗ VPN连接失败，目标IP不可达"
        echo "请先手动连接VPN:"
        echo "  ./vpn_connect_full.sh"
        echo ""
        echo "或者使用Mock模式部署:"
        echo "  ./deploy.sh --mockAlgo"
        echo ""
        echo "部署已终止"
        exit 1
    fi
fi

if [ "$ALGO_SMALL_SCALE_TEST" = true ]; then
    echo "Algo 小流量测试已开启"
    export ALGO_SMALL_SCALE_TEST=true
else
    echo "Algo 小流量测试未开启"
    export ALGO_SMALL_SCALE_TEST=false
fi

if [ "$USE_80_PORT" = true ]; then
    echo "应用端口已设置为 80"
    export SERVER_PORT=80
else
    echo "使用默认端口 8080"
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