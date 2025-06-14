package com.taichu.gateway.web.user;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.service.UserAppService;
import com.taichu.gateway.web.user.dto.AuthDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@Api(tags = "用户接口")
public class UserController {

    private final UserAppService userAppService;

    public UserController(UserAppService userAppService) {
        this.userAppService = userAppService;
    }

    @PostMapping("/login")
    @ApiOperation(value = "短信验证码登录", notes = "如果用户不存在则自动注册")
    public SingleResponse<AuthDTO> login(
            @ApiParam(required = true) @RequestParam String phone,
            @ApiParam(required = true) @RequestParam String verifyCode) {
        AuthDTO authDTO = userAppService.login(phone, verifyCode);
        return SingleResponse.of(authDTO);
    }

    @GetMapping("get-active-workflow")
    public SingleResponse<WorkflowDTO> getWorkflow(@RequestParam Long userId) {
        return SingleResponse.buildSuccess();
    }
}
