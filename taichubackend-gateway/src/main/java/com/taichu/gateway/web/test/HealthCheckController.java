package com.taichu.gateway.web.test;

import com.alibaba.cola.dto.SingleResponse;
import org.springframework.web.bind.annotation.*;

/**
 * 心跳检查控制器
 */
@RestController
@RequestMapping("/api/test")
public class HealthCheckController {

    @GetMapping("/healthCheck")
    public SingleResponse<String> healthCheck() {
        return SingleResponse.of("hi there");
    }

}
