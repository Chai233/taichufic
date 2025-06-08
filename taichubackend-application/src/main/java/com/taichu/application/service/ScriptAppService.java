package com.taichu.application.service;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.executor.ScriptTaskExecutor;
import com.taichu.application.helper.WorkflowValidationHelper;
import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicAlgoTaskRepository;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import com.taichu.sdk.model.GenerateScriptRequest;
import com.taichu.sdk.model.TaskStatusDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ScriptAppService {

    @Autowired
    private WorkflowValidationHelper workflowValidationHelper;

    @Autowired
    private ScriptTaskExecutor scriptTaskExecutor;

    @Autowired
    private FicAlgoTaskRepository ficAlgoTaskRepository;

    @Autowired
    private FicWorkflowRepository ficWorkflowRepository;
    @Autowired
    private FicWorkflowTaskRepository ficWorkflowTaskRepository;

    public SingleResponse<Long> submitGenScriptTask(GenerateScriptRequest request, Long userId) {
        // 1. 校验工作流状态
        SingleResponse<?> validateResponse = workflowValidationHelper.validateWorkflow(
            request.getWorkflowId(), 
            userId,
            WorkflowStatusEnum.UPLOAD_FILE_DONE
        );
        if (!validateResponse.isSuccess()) {
            return SingleResponse.buildFailure(validateResponse.getErrCode(), validateResponse.getErrMessage());
        }

        // 2. 提交任务
        return scriptTaskExecutor.submitTask(request.getWorkflowId());
    }

    public SingleResponse<TaskStatusDTO> getScriptTaskStatus(Long workflowId) {
        // 1. 查询任务
        FicWorkflowTaskBO task = ficWorkflowTaskRepository.findByWorkflowIdAndTaskType(workflowId, TaskTypeEnum.SCRIPT_GENERATION.name());
        if (task == null) {
            return SingleResponse.buildFailure("TASK_NOT_FOUND", "任务不存在");
        }

        // 2. 构建返回结果
        TaskStatusDTO statusDTO = new TaskStatusDTO();
        statusDTO.setTaskId(task.getId());
        statusDTO.setStatus(task.getStatus());

        return SingleResponse.of(statusDTO);
    }
}
