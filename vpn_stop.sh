#!/bin/bash

TARGET_IP="192.168.100.106"

echo "关闭 VPN 进程..."
sudo pkill -f openfortivpn

echo "删除静态路由..."
sudo ip route del $TARGET_IP

echo "VPN 已断开，路由已清理。"
