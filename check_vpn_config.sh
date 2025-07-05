#!/bin/bash

echo "=== VPN 配置检查脚本 ==="

# 检查openfortivpn是否安装
echo "1. 检查 openfortivpn 是否安装..."
if command -v openfortivpn >/dev/null 2>&1; then
    echo "✓ openfortivpn 已安装"
    openfortivpn --version
else
    echo "✗ openfortivpn 未安装"
    echo "请安装 openfortivpn:"
    echo "  Ubuntu/Debian: sudo apt-get install openfortivpn"
    echo "  CentOS/RHEL: sudo yum install openfortivpn"
fi

echo ""

# 检查可能的配置文件位置
echo "2. 检查VPN配置文件..."
CONFIG_LOCATIONS=(
    "/etc/openfortivpn/config"
    "/etc/openfortivpn/openfortivpn.conf"
    "~/.openfortivpn/config"
    "~/.openfortivpn/openfortivpn.conf"
    "./openfortivpn.conf"
    "./config"
)

CONFIG_FOUND=false
for config in "${CONFIG_LOCATIONS[@]}"; do
    expanded_config=$(eval echo "$config")
    if [ -f "$expanded_config" ]; then
        echo "✓ 找到配置文件: $expanded_config"
        echo "  文件权限: $(ls -la "$expanded_config")"
        CONFIG_FOUND=true
    fi
done

if [ "$CONFIG_FOUND" = false ]; then
    echo "✗ 未找到VPN配置文件"
    echo "请创建配置文件，示例内容:"
    echo "host = your-vpn-server.com"
    echo "port = 443"
    echo "username = your-username"
    echo "password = your-password"
    echo "trusted-cert = 88aa3e46ec12aa820612340e795c6f340ef7cb9193e74e537ae3e2412a72ba69"
fi

echo ""

# 检查网络接口
echo "3. 检查网络接口..."
echo "当前网络接口:"
ip addr show | grep -E "^[0-9]+:" | awk '{print $2}' | sed 's/://'

echo ""
echo "检查VPN相关接口:"
for iface in ppp0 ppp1 tun0 tun1; do
    if ip addr show "$iface" >/dev/null 2>&1; then
        echo "✓ VPN接口 $iface 存在"
        ip addr show "$iface" | grep "inet "
    else
        echo "✗ VPN接口 $iface 不存在"
    fi
done

echo ""

# 检查路由表
echo "4. 检查路由表..."
echo "当前路由表:"
ip route show

echo ""

# 检查VPN进程
echo "5. 检查VPN进程..."
VPN_PROCESSES=$(pgrep -f openfortivpn)
if [ -n "$VPN_PROCESSES" ]; then
    echo "✓ 发现VPN进程:"
    for pid in $VPN_PROCESSES; do
        echo "  PID: $pid"
        ps -p "$pid" -o pid,ppid,cmd
    done
else
    echo "✗ 未发现VPN进程"
fi

echo ""

# 检查日志文件
echo "6. 检查VPN日志..."
if [ -f "/tmp/vpn.log" ]; then
    echo "✓ VPN日志文件存在: /tmp/vpn.log"
    echo "最后10行日志:"
    tail -10 /tmp/vpn.log
else
    echo "✗ VPN日志文件不存在: /tmp/vpn.log"
fi

echo ""

# 测试网络连接
echo "7. 测试网络连接..."
echo "测试基本网络连接 (8.8.8.8):"
if ping -c 1 8.8.8.8 >/dev/null 2>&1; then
    echo "✓ 基本网络连接正常"
else
    echo "✗ 基本网络连接失败"
fi

echo "测试目标IP连接 (192.168.100.106):"
if ping -c 1 192.168.100.106 >/dev/null 2>&1; then
    echo "✓ 目标IP可达"
else
    echo "✗ 目标IP不可达"
fi

echo ""

# 检查sudo权限
echo "8. 检查sudo权限..."
if sudo -n true 2>/dev/null; then
    echo "✓ 具有sudo权限"
else
    echo "✗ 需要sudo权限"
    echo "请确保当前用户具有sudo权限"
fi

echo ""
echo "=== 检查完成 ===" 