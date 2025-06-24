#!/bin/bash

TARGET_IP="192.168.100.106"
VPN_IF="ppp0"
PID_FILE="/tmp/vpn_monitor.pid"

if [ -f $PID_FILE ]; then
    MONITOR_PID=$(cat $PID_FILE)
    echo "停止 VPN 守护进程 (PID: $MONITOR_PID)..."
    kill $MONITOR_PID
    rm -f $PID_FILE
else
    echo "找不到 VPN 守护进程 PID 文件，可能已经停止。"
fi

echo "关闭 VPN 进程..."
sudo pkill -f openfortivpn

echo "删除静态路由..."
sudo ip route del "$TARGET_IP" dev "$VPN_IF" 2>/dev/null || echo "路由已不存在"

echo "VPN 已断开，路由已清理。"
