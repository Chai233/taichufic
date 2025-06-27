package com.taichu.infra.sms;

import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dysmsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsResponse;
import darabonba.core.client.ClientOverrideConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;

/**
 * 阿里云短信服务
 */
@Service
@Slf4j
public class AliCloudSmsService {
    
    @Value("${aliyun.sms.sign-name:}")
    private String signName;
    
    @Value("${aliyun.sms.template-code:}")
    private String templateCode;
    
    @Value("${aliyun.sms.region:cn-hangzhou}")
    private String region;
    
    private AsyncClient client;
    
    /**
     * 初始化客户端
     */
    @PostConstruct
    public void init() {
        try {
            // 配置认证信息
            StaticCredentialProvider provider = StaticCredentialProvider.create(Credential.builder()
                    .accessKeyId(System.getenv("ALIBABA_CLOUD_ACCESS_KEY_ID"))
                    .accessKeySecret(System.getenv("ALIBABA_CLOUD_ACCESS_KEY_SECRET"))
                    .build());

            // 配置客户端
            client = AsyncClient.builder()
                    .region(region)
                    .credentialsProvider(provider)
                    .overrideConfiguration(
                            ClientOverrideConfiguration.create()
                                    .setEndpointOverride("dysmsapi.aliyuncs.com")
                    )
                    .build();
            
            log.info("阿里云短信服务客户端初始化成功: region={}, signName={}, templateCode={}", 
                    region, signName, templateCode);
        } catch (Exception e) {
            log.error("阿里云短信服务客户端初始化失败", e);
            throw new RuntimeException("阿里云短信服务初始化失败", e);
        }
    }
    
    /**
     * 发送短信验证码
     * @param phoneNumber 手机号
     * @param verifyCode 验证码
     * @return 是否发送成功
     */
    public boolean sendVerifyCode(String phoneNumber, String verifyCode) {
        try {
            if (client == null) {
                log.error("短信客户端未初始化");
                return false;
            }
            
            // 构建短信请求
            SendSmsRequest request = SendSmsRequest.builder()
                    .phoneNumbers(phoneNumber)
                    .signName(signName)
                    .templateCode(templateCode)
                    .templateParam("{\"code\":\"" + verifyCode + "\"}")
                    .build();
            
            // 发送短信
            CompletableFuture<SendSmsResponse> response = client.sendSms(request);
            SendSmsResponse resp = response.get();
            
            log.info("发送短信结果: phoneNumber={}, code={}, response={}", phoneNumber, verifyCode, resp.getBody());
            
            // 检查发送结果
            return "OK".equals(resp.getBody().getCode());
            
        } catch (Exception e) {
            log.error("发送短信失败: phoneNumber={}, code={}", phoneNumber, verifyCode, e);
            return false;
        }
    }
    
    /**
     * 关闭客户端
     */
    @PreDestroy
    public void close() {
        if (client != null) {
            try {
                client.close();
                log.info("阿里云短信服务客户端已关闭");
            } catch (Exception e) {
                log.error("关闭阿里云短信服务客户端失败", e);
            }
        }
    }
} 