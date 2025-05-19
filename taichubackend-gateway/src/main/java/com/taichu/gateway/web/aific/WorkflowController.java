package com.taichu.gateway.web.aific;

import com.alibaba.cola.dto.SingleResponse;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflow")
@Api(tags = "工作流功能性接口")
public class WorkflowController {

    @PostMapping("/create")
    public SingleResponse<Long> createWorkflow() {
        // TODO 创建workflow
        return SingleResponse.buildSuccess();
    }
}

