package com.taichu.gateway.web.workflow;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.service.WorkflowAppService;
import com.taichu.application.service.user.util.AuthUtil;
import com.taichu.sdk.model.WorkflowDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflow")
@Api(tags = "工作流功能性接口")
public class WorkflowController {

    @Autowired
    private WorkflowAppService workflowAppService;

    @PostMapping("/create")
    @ApiOperation(value = "创建工作流", notes = "创建新的工作流，如果用户有未完成的工作流，会先将其关闭")
    public SingleResponse<Long> createWorkflow() {
        Long userId = AuthUtil.getCurrentUserId();
        return workflowAppService.createWorkflow(userId);
    }


    @GetMapping("get-active-workflow")
    public SingleResponse<WorkflowDTO> getWorkflow() {
        Long userId = AuthUtil.getCurrentUserId();
        return workflowAppService.getValidWorkflow(userId);
    }
}

