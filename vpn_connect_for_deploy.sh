#!/bin/bash

TARGET_IP="192.168.100.106"
VPN_IF="ppp0"
VPN_CONFIG="/etc/openfortivpn/config"
TRUSTED_CERT="88aa3e46ec12aa820612340e795c6f340ef7cb9193e74e537ae3e2412a72ba69"
VPN_LOG="/tmp/vpn.log"
PID_FILE="/tmp/vpn_monitor.pid"

function start_vpn_and_wait() {
    echo "启动 VPN..."
    
    # 清理可能存在的VPN进程
    sudo pkill -f openfortivpn 2>/dev/null
    sleep 2
    
    # 启动VPN
    sudo openfortivpn -c $VPN_CONFIG --trusted-cert $TRUSTED_CERT --persistent=60 >> $VPN_LOG 2>&1 &
    VPN_PID=$!
    echo "VPN 进程已启动 (PID: $VPN_PID)"
    
    # 等待VPN接口建立
    echo "等待VPN接口建立..."
    for i in {1..60}; do
        if ! ps -p "$VPN_PID" > /dev/null; then
            echo "VPN 进程已退出，启动失败。请检查日志：$VPN_LOG"
            return 1
        fi
        
        # 检查VPN接口
        if ip addr show "$VPN_IF" 2>/dev/null | grep -q "inet "; then
            echo "VPN 接口 $VPN_IF 已上线"
            break
        fi
        
        # 检查其他可能的VPN接口名
        for iface in ppp0 ppp1 tun0 tun1; do
            if ip addr show "$iface" 2>/dev/null | grep -q "inet "; then
                echo "发现VPN接口: $iface"
                VPN_IF="$iface"
                break 2
            fi
        done
        
        echo "等待 VPN 接口上线 (${i}/60 秒)..."
        sleep 1
    done
    
    if ! ip addr show "$VPN_IF" 2>/dev/null | grep -q "inet "; then
        echo "VPN 接口启动失败，准备关闭 VPN..."
        sudo kill "$VPN_PID" 2>/dev/null
        return 1
    fi
    
    # 添加路由
    echo "添加静态路由..."
    sudo ip route add "$TARGET_IP" dev "$VPN_IF" 2>/dev/null || echo "静态路由已存在或添加失败"
    
    # 等待路由生效并测试连接
    echo "等待路由生效并测试连接..."
    for i in {1..30}; do
        if ping -c 1 "$TARGET_IP" >/dev/null 2>&1; then
            echo "✓ VPN连接成功，目标IP可达"
            return 0
        fi
        echo "等待连接建立 (${i}/30 秒)..."
        sleep 1
    done
    
    echo "⚠ VPN接口已建立，但目标IP不可达"
    return 1
}

function start_monitor() {
    echo "启动VPN监控进程..."
    while true; do
        if ! pgrep -f "openfortivpn" > /dev/null; then
            echo "$(date) VPN 掉线，自动重连..." >> "$VPN_LOG"
            start_vpn_and_wait
        fi
        sleep 15
    done
}

# 主执行逻辑
echo "=== 部署专用VPN连接脚本 ==="
echo "目标IP: $TARGET_IP"
echo "VPN接口: $VPN_IF"

# 启动VPN并等待连接建立
if start_vpn_and_wait; then
    echo "VPN连接成功，启动监控进程..."
    
    # 启动守护进程并记录 PID
    start_monitor &
    echo $! > "$PID_FILE"
    echo "VPN 守护进程已启动，PID: $(cat $PID_FILE)"
    echo "监控日志: $VPN_LOG"
    
    # 再次确认连接
    echo "最终确认VPN连接..."
    if ping -c 3 "$TARGET_IP" >/dev/null 2>&1; then
        echo "✓ VPN连接确认成功，可以继续部署"
        exit 0
    else
        echo "✗ VPN连接确认失败"
        exit 1
    fi
else
    echo "✗ VPN连接失败"
    echo "请检查以下内容:"
    echo "1. VPN配置文件是否存在: $VPN_CONFIG"
    echo "2. 网络连接是否正常"
    echo "3. VPN服务是否可用"
    echo "4. 查看详细日志: $VPN_LOG"
    exit 1
fi 