package com.taichu.gateway.web.workflow;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.service.WorkflowAppService;
import com.taichu.application.service.WorkflowRollbackAppService;
import com.taichu.application.service.WorkflowRollbackTargetEnum;
import com.taichu.application.service.user.util.AuthUtil;
import com.taichu.sdk.model.WorkflowDTO;
import com.taichu.common.common.exception.ControllerExceptionHandle;
import com.taichu.sdk.model.request.WorkflowRollBackRequest;
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
    @Autowired
    private WorkflowRollbackAppService workflowRollbackAppService;

    /**
     * 创建工作流
     * 如果用户有未完成的工作流，会先将其关闭
     *
     * @return 工作流ID
     */
    @PostMapping("/create")
    @ControllerExceptionHandle(biz = "CREATE_WORKFLOW")
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
    @ControllerExceptionHandle(biz = "GET_ACTIVE_WORKFLOW")
    public SingleResponse<WorkflowDTO> getWorkflow() {
        Long userId = AuthUtil.getCurrentUserId();
        return workflowAppService.getValidWorkflow(userId);
    }

    @PostMapping("roll-back-to-target-status")
    @ControllerExceptionHandle(biz = "ROLL_BACK_TO_TARGET_STATUS")
    public SingleResponse<Void> rollbackWorkflow(@RequestBody WorkflowRollBackRequest request) throws Exception {
        Long userId = AuthUtil.getCurrentUserId();

        // 直接关闭当前workflow
        if ("INIT_WAIT_FOR_FILE".equals(request.getTargetStatus())) {
            Long workflowId = request.getWorkflowId();
            return workflowAppService.closeWorkflow(workflowId, userId);
        }

        return workflowRollbackAppService.rollbackWorkflow(userId,
                request.getWorkflowId(),
                WorkflowRollbackTargetEnum.findByValue(request.getTargetStatus()));
    }

    /**
     * 关闭工作流
     * 关闭工作流时，会将所有关联资源的状态都改为invalid
     *
     * @param workflowId 工作流ID
     * @return 关闭结果
     */
    @PostMapping("close")
    @ControllerExceptionHandle(biz = "CLOSE_WORKFLOW")
    public SingleResponse<Void> closeWorkflow(@RequestParam Long workflowId) {
        Long userId = AuthUtil.getCurrentUserId();
        return workflowAppService.closeWorkflow(workflowId, userId);
    }
}

