package com.taichu.gateway.web.aific;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.gateway.model.ComposeVideoRequest;
import com.taichu.gateway.model.FullVideoListItemDTO;
import com.taichu.gateway.model.TaskStatusDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
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

