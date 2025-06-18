package com.taichu.gateway.web.user;

import com.alibaba.cola.dto.SingleResponse;
import org.springframework.web.bind.annotation.*;

/**
 * 用户相关接口控制器
 */
@RestController
@RequestMapping("/api/test")
public class HealthCheckController {

    @GetMapping("/healthCheck")
    public SingleResponse<String> login() {
        return SingleResponse.of("hi there");
    }

}
