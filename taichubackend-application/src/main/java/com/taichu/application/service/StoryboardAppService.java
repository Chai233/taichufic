package com.taichu.application.service;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.executor.ScriptTaskExecutor;
import com.taichu.application.executor.StoryboardTaskExecutor;
import com.taichu.application.helper.WorkflowValidationHelper;
import com.taichu.domain.enums.TaskTypeEnum;
import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicAlgoTaskRepository;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import com.taichu.sdk.model.GenerateScriptRequest;
import com.taichu.sdk.model.GenerateStoryboardRequest;
import com.taichu.sdk.model.ScriptDTO;
import com.taichu.sdk.model.TaskStatusDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StoryboardAppService {

    @Autowired
    private WorkflowValidationHelper workflowValidationHelper;

    @Autowired
    private StoryboardTaskExecutor storyboardTaskExecutor;


    public SingleResponse<Long> generateStoryboard(GenerateStoryboardRequest request, Long userId) {
        // 1. 校验工作流状态
        SingleResponse<?> validateResponse = workflowValidationHelper.validateWorkflow(request.getWorkflowId(), userId, WorkflowStatusEnum.SCRIPT_GEN);
        if (!validateResponse.isSuccess()) {
            return SingleResponse.buildFailure(validateResponse.getErrCode(), validateResponse.getErrMessage());
        }

        // 2. 提交任务
        return storyboardTaskExecutor.submitTask(request.getWorkflowId());
    }
}
