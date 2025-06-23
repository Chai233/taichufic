#!/bin/bash
# --- 配置项 ---
VPN_SERVER="112.25.93.66:8443"      # VPN服务器地址，改成你的
VPN_USER="beiyin"      # VPN用户名，改成你的
VPN_PASS="ZjSau6n75T9wkfpc"      # VPN密码（明文）
TARGET_IP="192.168.100.106"       # 需要走VPN的目标IP
VPN_IF="ppp0"                     # VPN接口，一般ppp0
NEW_USER="beiyin"                 # 新建用户
NEW_USER_PASS="1234"   # 新用户密码，改成你想要的

# --- 日志函数 ---
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# --- 清理函数 ---
cleanup() {
    log "清理VPN连接和路由..."
    pkill -f "fortivpn" 2>/dev/null
    ip route del $TARGET_IP dev $VPN_IF 2>/dev/null
}

# --- 信号处理 ---
trap cleanup EXIT INT TERM

# --- 检查依赖 ---
if ! command -v expect &> /dev/null; then
    log "错误: expect 未安装，请先安装: apt-get install expect"
    exit 1
fi

if [ ! -f "/opt/forticlient/fortivpn" ]; then
    log "错误: FortiClient 未找到在 /opt/forticlient/fortivpn"
    exit 1
fi

# --- 创建普通用户 ---
if id "$NEW_USER" &>/dev/null; then
    log "用户 $NEW_USER 已存在"
else
    log "创建普通用户 $NEW_USER ..."
    adduser --gecos "" --disabled-password $NEW_USER
    echo "${NEW_USER}:${NEW_USER_PASS}" | chpasswd
    usermod -aG sudo $NEW_USER
    log "用户 $NEW_USER 创建完成"
fi

# --- 写普通用户的VPN连接脚本 ---
VPN_SCRIPT="/home/${NEW_USER}/connect_vpn.sh"
cat > $VPN_SCRIPT << 'SCRIPT_EOF'
#!/bin/bash
VPN_SERVER="$1"
VPN_USER="$2"

# 启动FortiClient VPN
/opt/forticlient/fortivpn -h "$VPN_SERVER" -u "$VPN_USER" --keepalive
SCRIPT_EOF

chmod +x $VPN_SCRIPT
chown $NEW_USER:$NEW_USER $VPN_SCRIPT

# --- 创建expect脚本用于自动输入密码 ---
EXPECT_SCRIPT="/tmp/vpn_connect_$$"
cat > $EXPECT_SCRIPT << EXPECT_EOF
#!/usr/bin/expect -f
set timeout 60
set vpn_server [lindex \$argv 0]
set vpn_user [lindex \$argv 1]
set vpn_pass [lindex \$argv 2]
set new_user [lindex \$argv 3]
set vpn_script [lindex \$argv 4]

# 启动VPN连接
spawn sudo -u \$new_user \$vpn_script \$vpn_server \$vpn_user

# 等待密码提示并输入密码
expect {
    "Password:" {
        send "\$vpn_pass\r"
        exp_continue
    }
    "STATUS::Tunnel running" {
        puts "VPN连接成功建立"
    }
    "connecting to" {
        exp_continue
    }
    "STATUS::Connecting" {
        exp_continue
    }
    timeout {
        puts "VPN连接超时"
        exit 1
    }
    eof {
        puts "VPN进程意外结束"
        exit 1
    }
}

# 保持expect脚本运行，维持VPN连接
expect eof
EXPECT_EOF

chmod +x $EXPECT_SCRIPT

# --- 在后台启动VPN连接 ---
log "开始在后台启动VPN连接..."
nohup $EXPECT_SCRIPT "$VPN_SERVER" "$VPN_USER" "$VPN_PASS" "$NEW_USER" "$VPN_SCRIPT" > /tmp/vpn_connect.log 2>&1 &
VPN_PID=$!

log "VPN连接进程已启动，PID: $VPN_PID"

# --- 等待VPN接口建立 ---
log "等待VPN接口 $VPN_IF 启动..."
VPN_READY=false
for i in {1..60}; do
    if ip link show $VPN_IF &>/dev/null; then
        # 检查接口是否有IP地址
        if ip addr show $VPN_IF | grep -q "inet "; then
            log "VPN接口 $VPN_IF 已启动并获得IP地址"
            VPN_READY=true
            break
        fi
    fi

    # 检查VPN进程是否还在运行
    if ! kill -0 $VPN_PID 2>/dev/null; then
        log "VPN进程已退出，检查日志: /tmp/vpn_connect.log"
        cat /tmp/vpn_connect.log
        exit 1
    fi

    sleep 2
done

if [ "$VPN_READY" != "true" ]; then
    log "VPN接口启动超时，检查日志:"
    cat /tmp/vpn_connect.log
    exit 1
fi

# --- 添加路由规则 ---
log "添加路由规则..."
if ip route add $TARGET_IP dev $VPN_IF 2>/dev/null; then
    log "路由添加成功: $TARGET_IP -> $VPN_IF"
else
    log "路由添加失败，可能路由已存在"
fi

# --- 显示当前路由状态 ---
log "当前相关路由信息:"
ip route show | grep -E "($TARGET_IP|$VPN_IF)"

# --- 显示VPN接口信息 ---
log "VPN接口信息:"
ip addr show $VPN_IF

# --- 保存进程信息 ---
echo $VPN_PID > /tmp/vpn_pid
log "VPN进程PID已保存到 /tmp/vpn_pid"

# --- 创建停止脚本 ---
STOP_SCRIPT="/home/${NEW_USER}/stop_vpn.sh"
cat > $STOP_SCRIPT << 'STOP_EOF'
#!/bin/bash
if [ -f /tmp/vpn_pid ]; then
    VPN_PID=$(cat /tmp/vpn_pid)
    if kill -0 $VPN_PID 2>/dev/null; then
        echo "停止VPN进程 $VPN_PID"
        kill $VPN_PID
        sleep 2
        kill -9 $VPN_PID 2>/dev/null
    fi
    rm -f /tmp/vpn_pid
fi

# 清理FortiClient进程
pkill -f "fortivpn" 2>/dev/null

# 清理路由
ip route del TARGET_IP dev VPN_IF 2>/dev/null

echo "VPN连接已停止"
STOP_EOF

# 替换停止脚本中的变量
sed -i "s/TARGET_IP/$TARGET_IP/g" $STOP_SCRIPT
sed -i "s/VPN_IF/$VPN_IF/g" $STOP_SCRIPT

chmod +x $STOP_SCRIPT
chown $NEW_USER:$NEW_USER $STOP_SCRIPT

# --- 清理临时文件 ---
rm -f $EXPECT_SCRIPT

log "脚本执行完毕！"
log "VPN连接在后台运行中，PID: $VPN_PID"
log "可以使用以下命令:"
log "  查看VPN日志: tail -f /tmp/vpn_connect.log"
log "  停止VPN连接: /home/${NEW_USER}/stop_vpn.sh"
log "  查看VPN进程: ps aux | grep fortivpn"