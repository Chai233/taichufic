package com.taichu.gateway.web.user;

import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.service.user.UserAppService;
import com.taichu.application.service.user.dto.AuthDTO;
import org.springframework.web.bind.annotation.*;

/**
 * 用户相关接口控制器
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserAppService userAppService;

    public UserController(UserAppService userAppService) {
        this.userAppService = userAppService;
    }

    /**
     * 短信验证码登录
     * 如果用户不存在则自动注册
     *
     * @param phone 手机号
     * @param verifyCode 验证码
     * @return 认证信息
     */
    @PostMapping("/login")
    public SingleResponse<AuthDTO> login(
            @RequestParam String phone,
            @RequestParam String verifyCode) {
        return userAppService.login(phone, verifyCode);
    }

    /**
     * 获取验证码（兼容旧接口）
     *
     * @param phone 手机号
     * @return 空响应
     */
    @PostMapping("/getVerificationCode")
    @ControllerExceptionHandle(biz = "GET_VERIFICATION_CODE")
    public SingleResponse<Void> getVerificationCode(@RequestParam String phone) {
        return userAppService.sendVerifyCode(phone);
    }
}
