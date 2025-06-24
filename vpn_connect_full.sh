#!/bin/bash

TARGET_IP="192.168.100.106"
VPN_IF="ppp0"
VPN_CONFIG="/etc/openfortivpn/config"
TRUSTED_CERT="88aa3e46ec12aa820612340e795c6f340ef7cb9193e74e537ae3e2412a72ba69"
VPN_LOG="/tmp/vpn.log"
PID_FILE="/tmp/vpn_monitor.pid"

function start_vpn() {
    echo "启动 VPN..."
    sudo openfortivpn -c $VPN_CONFIG --trusted-cert $TRUSTED_CERT --persistent=60 >> $VPN_LOG 2>&1 &

    VPN_PID=$!

    echo "VPN 进程已启动 (PID: $VPN_PID)，等待接口上线..."

    for i in {1..30}; do
        if ! ps -p "$VPN_PID" > /dev/null; then
            echo "VPN 进程已退出，启动失败。请检查日志：$VPN_LOG"
            return 1
        fi

        if ip addr show "$VPN_IF" | grep -q "inet "; then
            echo "VPN 接口 $VPN_IF 已上线"
            break
        fi

        echo "等待 VPN 接口上线 (${i} 秒)..."
        sleep 1
    done

    if ! ip addr show "$VPN_IF" | grep -q "inet "; then
        echo "VPN 接口 $VPN_IF 启动失败，准备关闭 VPN..."
        sudo kill "$VPN_PID"
        return 1
    fi

    echo "添加静态路由..."
    sudo ip route add "$TARGET_IP" dev "$VPN_IF" || echo "静态路由已存在或添加失败"

    echo "VPN 已连接，日志：$VPN_LOG"
    return 0
}

function monitor_vpn() {
    while true; do
        if ! pgrep -f "openfortivpn" > /dev/null; then
            echo "$(date) VPN 掉线，自动重连..." >> "$VPN_LOG"
            start_vpn
        fi
        sleep 15
    done
}

# 启动守护进程并记录 PID
monitor_vpn &
echo $! > "$PID_FILE"

echo "VPN 守护进程已启动，PID: $(cat $PID_FILE)"
