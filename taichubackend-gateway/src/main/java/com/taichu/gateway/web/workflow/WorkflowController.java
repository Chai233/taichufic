package com.taichu.gateway.web.workflow;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.service.WorkflowAppService;
import com.taichu.application.service.user.util.AuthUtil;
import com.taichu.sdk.model.WorkflowDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 工作流相关接口控制器
 */
@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    @Autowired
    private WorkflowAppService workflowAppService;

    /**
     * 创建工作流
     * 如果用户有未完成的工作流，会先将其关闭
     *
     * @return 工作流ID
     */
    @PostMapping("/create")
    public SingleResponse<Long> createWorkflow() {
        Long userId = AuthUtil.getCurrentUserId();
        return workflowAppService.createWorkflow(userId);
    }

    /**
     * 获取当前活跃的工作流
     *
     * @return 工作流信息
     */
    @GetMapping("get-active-workflow")
    public SingleResponse<WorkflowDTO> getWorkflow() {
        Long userId = AuthUtil.getCurrentUserId();
        return workflowAppService.getValidWorkflow(userId);
    }
}

