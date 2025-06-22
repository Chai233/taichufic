package com.taichu.application.service.inner.algo.v2;

import com.taichu.application.service.inner.algo.v2.context.AlgoTaskContext;
import com.taichu.application.service.inner.algo.v2.context.StoryboardImgTaskContext;
import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.WorkflowTaskConstant;
import com.taichu.domain.model.*;
import com.taichu.infra.repo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class SingleStoryboardImgAlgoTaskProcessorV2 extends StoryboardImgAlgoTaskProcessorV2 {

    public SingleStoryboardImgAlgoTaskProcessorV2(FicStoryboardRepository ficStoryboardRepository,
                                                 FicRoleRepository ficRoleRepository,
                                                 AlgoGateway algoGateway,
                                                 FileGateway fileGateway,
                                                 FicResourceRepository ficResourceRepository,
                                                 FicWorkflowTaskRepository ficWorkflowTaskRepository,
                                                 FicWorkflowRepository ficWorkflowRepository) {
        super(ficStoryboardRepository, algoGateway, ficRoleRepository, fileGateway, ficResourceRepository, ficWorkflowTaskRepository, ficWorkflowRepository);
    }

    @Override
    public AlgoTaskTypeEnum getAlgoTaskType() {
        return AlgoTaskTypeEnum.USER_RETRY_SINGLE_STORYBOARD_IMG_GENERATION;
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
        StoryboardImgTaskContext context = new StoryboardImgTaskContext();
        context.setWorkflowId(workflowTask.getWorkflowId());
        context.setWorkflowTaskId(workflowTask.getId());
        context.setStoryboard(storyboard);
        context.setRoles(roles);
        // 这里可根据需要设置imageStyle/scale/styleScale
        return List.of(context);
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask, AlgoTaskContext context) throws Exception {
        // 处理单分镜图片生成成功后的逻辑
        log.info("[SingleStoryboardImgAlgoTaskProcessorV2.singleTaskSuccessPostProcess] 单分镜图片生成成功, algoTaskId: {}", algoTask.getAlgoTaskId());
        
        // 调用父类的处理逻辑
        super.singleTaskSuccessPostProcess(algoTask, context);
    }

    @Override
    public void singleTaskFailedPostProcess(FicAlgoTaskBO algoTask, AlgoTaskContext context, Exception e) {
        log.error("[SingleStoryboardImgAlgoTaskProcessorV2.singleTaskFailedPostProcess] 单分镜图片生成任务失败: {}", 
            algoTask.buildSummary(), e);
    }

    @Override
    public void postProcessAllComplete(FicWorkflowTaskBO workflowTask, List<AlgoTaskContext> contexts) {
        log.info("[SingleStoryboardImgAlgoTaskProcessorV2.postProcessAllComplete] 单分镜图片生成任务完成, workflowId: {}", workflowTask.getWorkflowId());
    }

    @Override
    public void postProcessAnyFailed(FicWorkflowTaskBO workflowTask, List<AlgoTaskContext> contexts) {
        super.postProcessAnyFailed(workflowTask, contexts);
    }

    @Override
    protected AlgoGateway getAlgoGateway() {
        return algoGateway;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
} 