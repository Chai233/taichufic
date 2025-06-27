# 短信功能使用说明

## 功能概述

本项目已集成阿里云短信服务，支持发送短信验证码和验证码校验功能。

## 架构设计

按照DDD分层架构设计：

- **Domain层**: `SmsGateway` 接口定义短信网关
- **Infra层**: `SmsGatewayImpl` 实现短信网关，调用 `AliCloudSmsService`
- **Application层**: `UserAppService` 处理业务逻辑，包含验证码缓存和校验

## 配置说明

### 环境变量配置

需要在环境变量中配置以下参数：

```bash
# 阿里云访问密钥
export ALIBABA_CLOUD_ACCESS_KEY_ID=your_access_key_id
export ALIBABA_CLOUD_ACCESS_KEY_SECRET=your_access_key_secret

# 短信服务配置（可选，有默认值）
export ALIYUN_SMS_SIGN_NAME=泰初科技
export ALIYUN_SMS_TEMPLATE_CODE=SMS_123456789
```

### 配置文件

在 `application-dev.yml` 和 `application-prod.yml` 中已添加短信配置：

```yaml
aliyun:
  sms:
    sign-name: ${ALIYUN_SMS_SIGN_NAME:泰初科技}
    template-code: ${ALIYUN_SMS_TEMPLATE_CODE:SMS_123456789}
    region: cn-qingdao
```

## API接口

### 1. 发送验证码

**接口**: `POST /api/v1/user/sendVerifyCode`

**参数**:
- `phone`: 手机号

**响应**: 成功返回空响应，失败返回错误信息

**限制**: 同一手机号1分钟内只能发送一次验证码

**错误码**:
- `SMS_0001`: 验证码发送过于频繁，请1分钟后再试
- `SMS_0002`: 短信发送失败，请稍后重试

### 2. 用户登录

**接口**: `POST /api/v1/user/login`

**参数**:
- `phone`: 手机号
- `verifyCode`: 验证码

**响应**: 返回认证信息

**验证**: 验证码5分钟内有效，验证成功后自动删除

**错误码**:
- `SMS_0003`: 验证码格式错误，请输入6位数字验证码
- `SMS_0004`: 验证码错误或已过期，请重新获取验证码

### 3. 兼容接口

**接口**: `POST /api/v1/user/getVerificationCode`

**功能**: 与 `sendVerifyCode` 功能相同，用于兼容旧版本

## 验证码校验机制

### 1. 发送流程
1. 检查1分钟内是否已发送过验证码
2. 生成6位数字验证码
3. 调用阿里云短信服务发送
4. 将验证码保存到本地缓存（5分钟有效期）

### 2. 校验流程
1. 验证输入格式是否为6位数字
2. 从缓存中获取该手机号的验证码
3. 比对验证码是否一致
4. 验证成功后立即删除缓存中的验证码（防止重复使用）

### 3. 安全特性
- **一次性使用**: 验证成功后立即删除，防止重复使用
- **时间限制**: 5分钟有效期，超时自动失效
- **频率限制**: 1分钟内不能重复发送
- **格式校验**: 严格校验6位数字格式

## 核心组件

### 1. VerifyCodeUtil

验证码生成工具类，生成6位数字验证码。

### 2. VerifyCodeCache

验证码缓存服务，提供：
- 验证码存储和获取
- 验证码校验（一次性使用）
- 发送频率限制（1分钟内不能重复发送）
- 自动过期清理（5分钟过期）

### 3. AliCloudSmsService

阿里云短信服务封装，基于阿里云SDK实现短信发送。

### 4. SmsGateway & SmsGatewayImpl

短信网关接口和实现，遵循DDD设计原则。

## 使用示例

```java
// 发送验证码
SingleResponse<Void> response = userAppService.sendVerifyCode("13800138000");

// 用户登录
SingleResponse<AuthDTO> authResponse = userAppService.login("13800138000", "123456");
```

## 注意事项

1. 验证码格式为6位数字
2. 验证码5分钟内有效
3. 同一手机号1分钟内只能发送一次验证码
4. 验证成功后验证码会自动删除，防止重复使用
5. 需要配置正确的阿里云访问密钥和短信模板
6. 验证码校验严格匹配，区分大小写 