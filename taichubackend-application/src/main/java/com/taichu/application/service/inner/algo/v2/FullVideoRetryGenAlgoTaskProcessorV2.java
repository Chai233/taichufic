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
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class FullVideoRetryGenAlgoTaskProcessorV2 extends AbstractAlgoTaskProcessorV2 {
    private final AlgoGateway algoGateway;
    private final FicStoryboardRepository ficStoryboardRepository;
    private final FileGateway fileGateway;
    private final FicResourceRepository ficResourceRepository;

    public FullVideoRetryGenAlgoTaskProcessorV2(AlgoGateway algoGateway,
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
        return AlgoTaskTypeEnum.USER_RETRY_FULL_VIDEO_GENERATION;
    }

    @Override
    public List<AlgoTaskContext> createTaskContextList(FicWorkflowTaskBO workflowTask) {
        Long workflowId = workflowTask.getWorkflowId();
        log.info("[FullVideoRetryGenAlgoTaskProcessorV2.createTaskContextList] 开始创建完整视频重试任务上下文, workflowId: {}", workflowId);
        
        List<FicStoryboardBO> storyboardBOS = ficStoryboardRepository.findValidByWorkflowId(workflowId);
        log.info("[FullVideoRetryGenAlgoTaskProcessorV2.createTaskContextList] 查询到分镜: {}", storyboardBOS);
        
        if (storyboardBOS.isEmpty()) {
            log.warn("[FullVideoRetryGenAlgoTaskProcessorV2.createTaskContextList] 分镜为空, workflowId: {}", workflowId);
            return List.of();
        }
        
        FullVideoTaskContext context = new FullVideoTaskContext();
        context.setWorkflowId(workflowId);
        context.setWorkflowTaskId(workflowTask.getId());
        context.setStoryboards(storyboardBOS);
        context.setVoiceType(Optional.ofNullable(workflowTask.getParams().get(WorkflowTaskConstant.VIDEO_VOICE_TYPE)).orElse("磁性男声"));
        context.setBgmType(Optional.ofNullable(workflowTask.getParams().get(WorkflowTaskConstant.VIDEO_BGM_TYPE)).orElse("摇滚质感"));
        
        return List.of(context);
    }

    @Override
    public void validateContext(AlgoTaskContext context) {
        FullVideoTaskContext fullVideoContext = (FullVideoTaskContext) context;
        if (fullVideoContext.getStoryboards() == null || fullVideoContext.getStoryboards().isEmpty()) {
            throw new IllegalArgumentException("分镜列表不能为空");
        }
        if (fullVideoContext.getWorkflowId() == null) {
            throw new IllegalArgumentException("工作流ID不能为空");
        }
    }

    @Override
    public AlgoTaskBOV2 generateAlgoTask(AlgoTaskContext context) {
        FullVideoTaskContext fullVideoContext = (FullVideoTaskContext) context;
        
        List<String> storyboardIds = StreamUtil.toStream(fullVideoContext.getStoryboards())
                .filter(Objects::nonNull)
                .map(FicStoryboardBO::getId)
                .map(Object::toString)
                .collect(Collectors.toList());
        
        log.info("[FullVideoRetryGenAlgoTaskProcessorV2.generateAlgoTask] 分镜ID列表: {}", storyboardIds);
        
        VideoMergeRequest request = new VideoMergeRequest();
        request.setWorkflow_id(String.valueOf(fullVideoContext.getWorkflowId()));
        request.setStoryboard_ids(storyboardIds);
        request.setVoice_type(fullVideoContext.getVoiceType());
        request.setBgm_type(fullVideoContext.getBgmType());
        
        log.info("[FullVideoRetryGenAlgoTaskProcessorV2.generateAlgoTask] 构建请求: {}", request);
        
        // 调用算法服务（使用基类的重试机制）
        String operationName = "Create full video retry generation task for workflow: " + fullVideoContext.getWorkflowId();
        AlgoResponse response = callAlgoServiceWithRetry(operationName, () -> algoGateway.createVideoMergeTask(request));
        
        if (response == null) {
            throw new RuntimeException("创建完整视频重试生成任务失败");
        }
        
        // 创建AlgoTaskBO
        AlgoTaskBOV2 algoTaskBO = new AlgoTaskBOV2();
        algoTaskBO.setAlgoTaskId(response.getTaskId());
        algoTaskBO.setRelevantId(fullVideoContext.getWorkflowId());
        algoTaskBO.setRelevantIdType(RelevanceType.WORKFLOW_ID);
        algoTaskBO.setTaskSummary("完整视频重试生成任务");
        
        return algoTaskBO;
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask, AlgoTaskContext context) throws Exception {
        log.info("[FullVideoRetryGenAlgoTaskProcessorV2.singleTaskSuccessPostProcess] 开始处理完整视频重试生成结果, algoTaskId: {}", algoTask.getAlgoTaskId());
        
        // 获取完整视频结果，添加重试逻辑
        String taskId = Objects.toString(algoTask.getAlgoTaskId());
        MultipartFile videoResult = retryGetResultOperation(
            () -> algoGateway.getVideoMergeResult(taskId),
            "getVideoMergeResult",
            taskId
        );
        
        if (videoResult == null) {
            throw new Exception("获取完整视频重试生成结果失败, algoTaskId: " + algoTask.getAlgoTaskId());
        }
        
        // 处理视频上传和资源保存逻辑
        processFullVideoResult(algoTask, videoResult);
    }

    @Override
    public void singleTaskFailedPostProcess(FicAlgoTaskBO algoTask, AlgoTaskContext context, Exception e) {
        log.error("[FullVideoRetryGenAlgoTaskProcessorV2.singleTaskFailedPostProcess] 完整视频重试生成任务失败: {}", 
            algoTask.buildSummary(), e);
    }

    @Override
    public void postProcessAllComplete(FicWorkflowTaskBO workflowTask, List<AlgoTaskContext> contexts) {
        log.info("[FullVideoRetryGenAlgoTaskProcessorV2.postProcessAllComplete] 完整视频重试生成任务完成, workflowId: {}", workflowTask.getWorkflowId());
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

    /**
     * 处理完整视频结果
     */
    private void processFullVideoResult(FicAlgoTaskBO algoTask, MultipartFile videoResult) throws Exception {
        FicWorkflowTaskBO workflowTask = ficWorkflowTaskRepository.findById(algoTask.getWorkflowTaskId());
        if (workflowTask == null) {
            throw new Exception("工作流任务不存在, workflowTaskId: " + algoTask.getWorkflowTaskId());
        }
        
        Long workflowId = workflowTask.getWorkflowId();
        String fileName = String.format("full_video_retry_%s_%s_%s", workflowId, algoTask.getAlgoTaskId(), videoResult.getOriginalFilename());
        log.info("[FullVideoRetryGenAlgoTaskProcessorV2.processFullVideoResult] 上传文件名: {}", fileName);
        
        Resp<String> uploadResp = fileGateway.saveFile(fileName, videoResult);
        if (!uploadResp.isSuccess()) {
            throw new Exception("上传完整视频重试结果到OSS失败, algoTaskId: " + algoTask.getAlgoTaskId() + ", error: " + uploadResp.getMessage());
        }
        
        // 删除旧的完整视频资源
        List<FicResourceBO> oldFullVideoResources = ficResourceRepository.findValidByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.FULL_VIDEO);
        for (FicResourceBO oldFullVideoResource : oldFullVideoResources) {
            ficResourceRepository.offlineResourceById(oldFullVideoResource.getId());
            log.info("[FullVideoRetryGenAlgoTaskProcessorV2.processFullVideoResult] 下线旧资源, resourceId: {}", oldFullVideoResource.getId());
        }
        
        // 保存新资源
        FicResourceBO ficResourceBO = new FicResourceBO();
        ficResourceBO.setWorkflowId(workflowId);
        ficResourceBO.setResourceType(ResourceTypeEnum.FULL_VIDEO.name());
        ficResourceBO.setResourceUrl(uploadResp.getData());
        ficResourceBO.setRelevanceId(algoTask.getRelevantId());
        ficResourceBO.setRelevanceType(algoTask.getRelevantIdType().toString());
        ficResourceBO.setResourceStorageType(ResourceStorageTypeEnum.ALI_CLOUD_OSS.name());
        ficResourceBO.setStatus(CommonStatusEnum.VALID.getValue());
        ficResourceBO.setGmtCreate(System.currentTimeMillis());
        ficResourceBO.setOriginName(videoResult.getOriginalFilename());
        ficResourceRepository.insert(ficResourceBO);
        
        log.info("[FullVideoRetryGenAlgoTaskProcessorV2.processFullVideoResult] 完整视频重试处理完成, algoTaskId: {}, resourceId: {}", 
            algoTask.getAlgoTaskId(), ficResourceBO.getId());
    }
} 