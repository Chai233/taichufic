package com.taichu.application.service.inner.algo.v2;

import com.taichu.application.service.inner.algo.v2.context.AlgoTaskContext;
import com.taichu.application.service.inner.algo.v2.context.StoryboardVideoTaskContext;
import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.WorkflowTaskConstant;
import com.taichu.domain.model.FicRoleBO;
import com.taichu.domain.model.FicStoryboardBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SingleStoryboardVideoAlgoTaskProcessorV2 extends StoryboardVideoAlgoTaskProcessorV2 {

    public SingleStoryboardVideoAlgoTaskProcessorV2(FicStoryboardRepository ficStoryboardRepository, AlgoGateway algoGateway, FicRoleRepository ficRoleRepository, FileGateway fileGateway, FicResourceRepository ficResourceRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository, FicWorkflowRepository ficWorkflowRepository) {
        super(ficStoryboardRepository, algoGateway, ficRoleRepository, fileGateway, ficResourceRepository, ficWorkflowTaskRepository, ficWorkflowRepository);
    }

    @Override
    public AlgoTaskTypeEnum getAlgoTaskType() {
        return AlgoTaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_VIDEO_GENERATION;
    }

    @Override
    public List<AlgoTaskContext> createTaskContextList(FicWorkflowTaskBO workflowTask) {
        String storyboardIdStr = workflowTask.getParams().get(WorkflowTaskConstant.STORYBOARD_ID);
        if (!StringUtils.isNumeric(storyboardIdStr)) {
            return List.of();
        }
        Long storyboardId = Long.parseLong(storyboardIdStr);
        FicStoryboardBO storyboard = ficStoryboardRepository.findById(storyboardId);
        if (storyboard == null) {
            return List.of();
        }
        List<FicRoleBO> roles = ficRoleRepository.findByWorkflowId(workflowTask.getWorkflowId());
        if (CollectionUtils.isEmpty(roles)) {
            return List.of();
        }
        
        StoryboardVideoTaskContext context = buildStoryboardVideoTaskContext(workflowTask, storyboard, roles);
        return List.of(context);
    }
} 