package com.taichu.application.service.inner.algo.v2;

import com.taichu.application.service.inner.algo.v2.context.AlgoTaskContext;
import com.taichu.application.service.inner.algo.v2.context.FullVideoTaskContext;
import com.taichu.common.common.model.Resp;
import com.taichu.common.common.util.StreamUtil;
import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.algo.model.request.VideoMergeRequest;
import com.taichu.domain.enums.*;
import com.taichu.domain.model.*;
import com.taichu.infra.repo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class FullVideoGenAlgoTaskProcessorV2 extends AbstractAlgoTaskProcessorV2 {

    private final AlgoGateway algoGateway;
    private final FicStoryboardRepository ficStoryboardRepository;
    private final FileGateway fileGateway;
    private final FicResourceRepository ficResourceRepository;

    public FullVideoGenAlgoTaskProcessorV2(AlgoGateway algoGateway,
                                         FicStoryboardRepository ficStoryboardRepository,
                                         FileGateway fileGateway,
                                         FicResourceRepository ficResourceRepository,
                                         FicWorkflowTaskRepository ficWorkflowTaskRepository,
                                         FicWorkflowRepository ficWorkflowRepository) {
        super(ficWorkflowTaskRepository, ficWorkflowRepository);
        this.algoGateway = algoGateway;
        this.ficStoryboardRepository = ficStoryboardRepository;
        this.fileGateway = fileGateway;
        this.ficResourceRepository = ficResourceRepository;
    }

    @Override
    public AlgoTaskTypeEnum getAlgoTaskType() {
        return AlgoTaskTypeEnum.FULL_VIDEO_GENERATION;
    }

    @Override
    public List<AlgoTaskContext> createTaskContextList(FicWorkflowTaskBO workflowTask) {
        Long workflowId = workflowTask.getWorkflowId();
        log.info("[FullVideoGenAlgoTaskProcessorV2.createTaskContextList] 开始创建完整视频任务上下文, workflowId: {}", workflowId);
        
        List<FicStoryboardBO> storyboardBOS = ficStoryboardRepository.findValidByWorkflowId(workflowId);
        if (storyboardBOS.isEmpty()) {
            log.warn("[FullVideoGenAlgoTaskProcessorV2.createTaskContextList] 分镜为空, workflowId: {}", workflowId);
            return List.of();
        }
        
        FullVideoTaskContext context = new FullVideoTaskContext();
        context.setWorkflowId(workflowId);
        context.setWorkflowTaskId(workflowTask.getId());
        context.setStoryboards(storyboardBOS);
        
        // 从工作流任务参数中获取配置
        Optional.ofNullable(workflowTask.getParams().get(WorkflowTaskConstant.VIDEO_VOICE_TYPE))
            .ifPresentOrElse(context::setVoiceType, () -> context.setVoiceType("磁性男声"));
        Optional.ofNullable(workflowTask.getParams().get(WorkflowTaskConstant.VIDEO_BGM_TYPE))
            .ifPresentOrElse(context::setBgmType, () -> context.setBgmType("摇滚质感"));
        
        return List.of(context);
    }

    @Override
    public void validateContext(AlgoTaskContext context) {
        FullVideoTaskContext videoContext = (FullVideoTaskContext) context;
        if (videoContext.getStoryboards() == null || videoContext.getStoryboards().isEmpty()) {
            throw new IllegalArgumentException("分镜列表不能为空");
        }
        if (videoContext.getWorkflowId() == null) {
            throw new IllegalArgumentException("工作流ID不能为空");
        }
        if (StringUtils.isEmpty(videoContext.getVoiceType())) {
            throw new IllegalArgumentException("voiceType不能为空");
        }
        if (StringUtils.isEmpty(videoContext.getBgmType())) {
            throw new IllegalArgumentException("BgmType不能为空");
        }
    }

    @Override
    public AlgoTaskBOV2 generateAlgoTask(AlgoTaskContext context) {
        FullVideoTaskContext videoContext = (FullVideoTaskContext) context;
        
        List<String> storyboardIds = StreamUtil.toStream(videoContext.getStoryboards())
                .filter(Objects::nonNull)
                .map(FicStoryboardBO::getId)
                .map(Object::toString)
                .collect(Collectors.toList());
        
        VideoMergeRequest request = new VideoMergeRequest();
        request.setWorkflow_id(String.valueOf(videoContext.getWorkflowId()));
        request.setStoryboard_ids(storyboardIds);
        request.setVoice_type(videoContext.getVoiceType());
        request.setBgm_type(videoContext.getBgmType());
        
        // 调用算法服务（使用基类的重试机制）
        String operationName = "Create full video task for workflow: " + videoContext.getWorkflowId();
        AlgoResponse response = callAlgoServiceWithRetry(operationName, () -> algoGateway.createVideoMergeTask(request));
        
        if (response == null) {
            throw new RuntimeException("创建完整视频任务失败");
        }
        
        // 创建AlgoTaskBO
        AlgoTaskBOV2 algoTaskBO = new AlgoTaskBOV2();
        algoTaskBO.setAlgoTaskId(response.getTaskId());
        algoTaskBO.setRelevantId(videoContext.getWorkflowId());
        algoTaskBO.setRelevantIdType(RelevanceType.WORKFLOW_ID);
        algoTaskBO.setTaskSummary(videoContext.getTaskSummary());
        
        return algoTaskBO;
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask, AlgoTaskContext context) throws Exception {
        FullVideoTaskContext videoContext = (FullVideoTaskContext) context;
        log.info("[FullVideoGenAlgoTaskProcessorV2.singleTaskSuccessPostProcess] 开始处理完整视频, algoTaskId: {}", algoTask.getAlgoTaskId());
        
        // 获取完整视频结果（使用基类的重试机制）
        String taskId = Objects.toString(algoTask.getAlgoTaskId());
        MultipartFile videoResult = retryGetResultOperation(
            () -> algoGateway.getVideoMergeResult(taskId),
            "getVideoMergeResult",
            taskId
        );
        
        if (videoResult == null) {
            throw new Exception("获取完整视频结果失败");
        }
        
        Long workflowId = videoContext.getWorkflowId();
        String fileName = String.format("full_video_%s_%s_%s", workflowId, algoTask.getAlgoTaskId(), videoResult.getName());
        
        // 上传到OSS
        Resp<String> uploadResp = fileGateway.saveFile(fileName, videoResult);
        if (!uploadResp.isSuccess()) {
            throw new Exception("上传完整视频到OSS失败");
        }
        
        // 删除旧的资源
        List<FicResourceBO> oldFullVideoResources = ficResourceRepository.findValidByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.FULL_VIDEO);
        for (FicResourceBO oldFullVideoResource : oldFullVideoResources) {
            ficResourceRepository.offlineResourceById(oldFullVideoResource.getId());
            log.info("[FullVideoGenAlgoTaskProcessorV2.singleTaskSuccessPostProcess] 下线旧资源, resourceId: {}", oldFullVideoResource.getId());
        }
        
        // 保存新的资源记录
        FicResourceBO ficResourceBO = new FicResourceBO();
        ficResourceBO.setWorkflowId(workflowId);
        ficResourceBO.setResourceType(ResourceTypeEnum.FULL_VIDEO.name());
        ficResourceBO.setResourceUrl(uploadResp.getData());
        ficResourceBO.setRelevanceId(algoTask.getRelevantId());
        ficResourceBO.setRelevanceType(algoTask.getRelevantIdType());
        ficResourceBO.setResourceStorageType(ResourceStorageTypeEnum.ALI_CLOUD_OSS.name());
        ficResourceBO.setStatus(CommonStatusEnum.VALID.getValue());
        ficResourceBO.setGmtCreate(System.currentTimeMillis());
        ficResourceBO.setOriginName(videoResult.getOriginalFilename());
        ficResourceRepository.insert(ficResourceBO);
        
        log.info("[FullVideoGenAlgoTaskProcessorV2.singleTaskSuccessPostProcess] 完整视频处理完成, algoTaskId: {}, resourceId: {}", algoTask.getAlgoTaskId(), ficResourceBO.getId());
    }

    @Override
    public void singleTaskFailedPostProcess(FicAlgoTaskBO algoTask, AlgoTaskContext context, Exception e) {
        log.error("[FullVideoGenAlgoTaskProcessorV2.singleTaskFailedPostProcess] 完整视频生成任务失败: {}", 
            algoTask.buildSummary(), e);
    }

    @Override
    public void postProcessAllComplete(FicWorkflowTaskBO workflowTask, List<AlgoTaskContext> contexts) {
        log.info("[FullVideoGenAlgoTaskProcessorV2.postProcessAllComplete] 完整视频生成任务完成, workflowId: {}", workflowTask.getWorkflowId());
    }

    @Override
    public void postProcessAnyFailed(FicWorkflowTaskBO workflowTask, List<AlgoTaskContext> contexts) {
        super.postProcessAnyFailed(workflowTask, contexts);
        Long workflowId = workflowTask.getWorkflowId();
        cleanupFailedFullVideoTask(workflowId);
    }

    private void cleanupFailedFullVideoTask(Long workflowId) {
        // 清理已创建的完整视频资源
        try {
            List<FicResourceBO> resources = ficResourceRepository.findValidByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.FULL_VIDEO);
            for (FicResourceBO resource : resources) {
                ficResourceRepository.offlineResourceById(resource.getId());
            }
            log.info("[cleanupFailedFullVideoTask] 清理失败任务资源成功, workflowId: {}", workflowId);
        } catch (Exception e) {
            log.error("[cleanupFailedFullVideoTask] 清理失败任务资源失败, workflowId: {}", workflowId, e);
        }
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
