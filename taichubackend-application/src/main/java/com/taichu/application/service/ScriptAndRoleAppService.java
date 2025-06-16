package com.taichu.application.service;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.annotation.EntranceLog;
import com.taichu.application.executor.RetryScriptTaskExecutor;
import com.taichu.application.executor.ScriptTaskExecutor;
import com.taichu.application.helper.WorkflowValidationHelper;
import com.taichu.common.common.exception.GlobalExceptionHandle;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ScriptAndRoleAppService {

    private final WorkflowValidationHelper workflowValidationHelper;
    private final ScriptTaskExecutor scriptTaskExecutor;
    private final RetryScriptTaskExecutor retryScriptTaskExecutor;
    private final FicWorkflowTaskRepository ficWorkflowTaskRepository;
    private final FicScriptRepository ficScriptRepository;
    private final FileGateway fileGateway;

    public ScriptAndRoleAppService(WorkflowValidationHelper workflowValidationHelper, ScriptTaskExecutor scriptTaskExecutor, RetryScriptTaskExecutor retryScriptTaskExecutor, FicWorkflowTaskRepository ficWorkflowTaskRepository, FicScriptRepository ficScriptRepository, FileGateway fileGateway) {
        this.workflowValidationHelper = workflowValidationHelper;
        this.scriptTaskExecutor = scriptTaskExecutor;
        this.retryScriptTaskExecutor = retryScriptTaskExecutor;
        this.ficWorkflowTaskRepository = ficWorkflowTaskRepository;
        this.ficScriptRepository = ficScriptRepository;
        this.fileGateway = fileGateway;
    }


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
    @EntranceLog(bizCode = "剧本生成")
    @GlobalExceptionHandle(biz = "剧本生成")
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
    @EntranceLog(bizCode = "查询剧本生成状态")
    @GlobalExceptionHandle(biz = "查询剧本生成状态")
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
    @EntranceLog(bizCode = "查询剧本")
    @GlobalExceptionHandle(biz = "查询剧本")
    public MultiResponse<ScriptVO> getScript(Long workflowId) {
        // 1. 获取剧本片段列表
        List<FicScriptBO> scriptBOs = ficScriptRepository.findByWorkflowId(workflowId);
        if (scriptBOs == null || scriptBOs.isEmpty()) {
            return MultiResponse.buildSuccess();
        }

        // 2. 转换为VO对象
        List<ScriptVO> scriptVOs = scriptBOs.stream()
                .map(scriptBO -> {
                    ScriptVO scriptVO = new ScriptVO();
                    scriptVO.setOrder(scriptBO.getOrderIndex());
                    scriptVO.setScriptContent(scriptBO.getContent());
                    return scriptVO;
                })
                .collect(Collectors.toList());

        return MultiResponse.of(scriptVOs);
    }

    /**
     * 重新生成剧本
     * @param request
     * @param userId
     * @return
     */
    @EntranceLog(bizCode = "剧本重新生成")
    @GlobalExceptionHandle(biz = "剧本重新生成")
    public SingleResponse<Long> submitReGenScriptTask(GenerateScriptRequest request, Long userId) {
        // 1. 校验工作流状态
        SingleResponse<?> validateResponse = workflowValidationHelper.validateWorkflow(
                request.getWorkflowId(),
                userId,
                WorkflowStatusEnum.SCRIPT_GEN_DONE
        );
        if (!validateResponse.isSuccess()) {
            return SingleResponse.buildFailure(validateResponse.getErrCode(), validateResponse.getErrMessage());
        }

        // 2. 提交任务
        return retryScriptTaskExecutor.submitTask(request.getWorkflowId(), request);
    }
}
