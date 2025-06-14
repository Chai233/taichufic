package com.taichu.application.service;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.executor.ScriptTaskExecutor;
import com.taichu.application.helper.WorkflowValidationHelper;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.model.FicScriptBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicScriptRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import com.taichu.sdk.model.request.GenerateScriptRequest;
import com.taichu.sdk.model.ScriptVO;
import com.taichu.sdk.model.WorkflowTaskStatusDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
public class ScriptAndStoryboardTextAppService {

    @Autowired
    private WorkflowValidationHelper workflowValidationHelper;

    @Autowired
    private ScriptTaskExecutor scriptTaskExecutor;

    @Autowired
    private FicWorkflowTaskRepository ficWorkflowTaskRepository;

    @Autowired
    private FicScriptRepository ficScriptRepository;

    @Autowired
    private FileGateway fileGateway;

    /**
     * 获取剧本资源
     * @param scriptId 剧本ID
     * @return 剧本资源
     */
    public Resource getScriptResource(Long scriptId) {
        FicScriptBO ficScriptBO = ficScriptRepository.findById(scriptId);
        if (ficScriptBO == null) {
            log.error("Script not found, scriptId: {}", scriptId);
            return null;
        }

        try {
            InputStream inputStream = fileGateway.getFileStream(ficScriptBO.getContent());
            return new InputStreamResource(inputStream);
        } catch (Exception e) {
            log.error("Failed to get script resource: {}", ficScriptBO.getContent(), e);
            return null;
        }
    }

    /**
     * 提交剧本生成任务
     * @param request
     * @param userId
     * @return
     */
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
        return scriptTaskExecutor.submitTask(request.getWorkflowId(), request);
    }

    /**
     * 查询剧本生成任务状态
     * @param workflowId
     * @return
     */
    public SingleResponse<WorkflowTaskStatusDTO> getScriptTaskStatus(Long workflowId) {
        // 1. 查询任务
        FicWorkflowTaskBO task = ficWorkflowTaskRepository.findByWorkflowIdAndTaskType(workflowId, TaskTypeEnum.SCRIPT_AND_ROLE_GENERATION.name());
        if (task == null) {
            return SingleResponse.buildFailure("TASK_NOT_FOUND", "任务不存在");
        }

        // 2. 构建返回结果
        WorkflowTaskStatusDTO statusDTO = new WorkflowTaskStatusDTO();
        statusDTO.setTaskId(task.getId());
        statusDTO.setStatus(TaskStatusEnum.fromCode(task.getStatus()).getDescription());

        return SingleResponse.of(statusDTO);
    }

    /**
     * 获取剧本
     * @param workflowId
     * @return
     */
    public MultiResponse<ScriptVO> getScript(Long workflowId) {
        // TODO@chai 获取剧本
        return MultiResponse.buildSuccess();
    }
}
