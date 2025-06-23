#!/bin/bash

# =========================
# 配置区
TARGET_IP="192.168.100.106"            # 需要走 VPN 的目标 IP
VPN_IF="ppp0"                          # VPN 接口名
VPN_CONFIG="/etc/openfortivpn/config"  # openfortivpn 配置文件路径
TRUSTED_CERT="88aa3e46ec12aa820612340e795c6f340ef7cb9193e74e537ae3e2412a72ba69"  # 你的 VPN 证书指纹
VPN_LOG="/tmp/vpn.log"                 # 日志文件路径
# =========================

echo "启动 VPN，并在后台运行..."
sudo openfortivpn -c $VPN_CONFIG --trusted-cert $TRUSTED_CERT > $VPN_LOG 2>&1 &

VPN_PID=$!

echo "VPN 进程已启动 (PID: $VPN_PID)，等待 VPN 接口 $VPN_IF 启动..."

# 循环等待 VPN 接口上线（最多等待 30 秒）
for i in {1..30}
do
    if ip addr show $VPN_IF > /dev/null 2>&1; then
        echo "VPN 接口 $VPN_IF 已上线"
        break
    else
        echo "等待 VPN 接口上线 ($i 秒)..."
        sleep 1
    fi
done

# 检查接口是否成功上线
if ! ip addr show $VPN_IF > /dev/null 2>&1; then
    echo "VPN 接口 $VPN_IF 未启动，退出。"
    exit 1
fi

echo "添加静态路由..."
sudo ip route add $TARGET_IP dev $VPN_IF

echo "VPN 已连接，路由已添加。"
echo "后台日志查看：tail -f $VPN_LOG"
