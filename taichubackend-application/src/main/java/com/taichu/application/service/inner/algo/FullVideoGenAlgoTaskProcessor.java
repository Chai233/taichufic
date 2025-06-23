package com.taichu.application.service.inner.algo;

import com.taichu.common.common.model.Resp;
import com.taichu.common.common.util.StreamUtil;
import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.algo.model.request.VideoMergeRequest;
import com.taichu.domain.enums.*;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicResourceBO;
import com.taichu.domain.model.FicStoryboardBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.repo.FicResourceRepository;
import com.taichu.infra.repo.FicStoryboardRepository;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class FullVideoGenAlgoTaskProcessor extends AbstractAlgoTaskProcessor {

    private final AlgoGateway algoGateway;
    private final FicWorkflowTaskRepository ficWorkflowTaskRepository;
    private final FicStoryboardRepository ficStoryboardRepository;
    private final FileGateway fileGateway;
    private final FicResourceRepository ficResourceRepository;

    @Autowired
    public FullVideoGenAlgoTaskProcessor(AlgoGateway algoGateway,
                                       FicWorkflowTaskRepository ficWorkflowTaskRepository,
                                       FicWorkflowRepository ficWorkflowRepository, 
                                       FicStoryboardRepository ficStoryboardRepository,
                                       FileGateway fileGateway,
                                       FicResourceRepository ficResourceRepository) {
        super(ficWorkflowTaskRepository, ficWorkflowRepository);
        this.algoGateway = algoGateway;
        this.ficWorkflowTaskRepository = ficWorkflowTaskRepository;
        this.ficStoryboardRepository = ficStoryboardRepository;
        this.fileGateway = fileGateway;
        this.ficResourceRepository = ficResourceRepository;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected AlgoGateway getAlgoGateway() {
        return algoGateway;
    }

    @Override
    public AlgoTaskTypeEnum getAlgoTaskType() {
        return AlgoTaskTypeEnum.FULL_VIDEO_GENERATION;
    }

    @Override
    public List<AlgoTaskBO> generateTasks(FicWorkflowTaskBO workflowTask) {
        Long workflowId = workflowTask.getWorkflowId();
        log.info("[FullVideoGenAlgoTaskProcessor.generateTasks] 开始生成完整视频任务, workflowId: {}", workflowId);
        try {
            List<FicStoryboardBO> storyboardBOS = ficStoryboardRepository.findValidByWorkflowId(workflowId);
            log.info("[FullVideoGenAlgoTaskProcessor.generateTasks] 查询到分镜: {}", storyboardBOS);
            List<String> storyboardIds = StreamUtil.toStream(storyboardBOS)
                    .filter(Objects::nonNull)
                    .map(FicStoryboardBO::getId)
                    .map(Object::toString)
                    .collect(Collectors.toList());
            log.info("[FullVideoGenAlgoTaskProcessor.generateTasks] 分镜ID列表: {}", storyboardIds);
            if (storyboardIds.isEmpty()) {
                log.error("[FullVideoGenAlgoTaskProcessor.generateTasks] 故事板ID列表为空, workflowId: {}", workflowId);
                return new ArrayList<>();
            }
            VideoMergeRequest request = new VideoMergeRequest();
            request.setWorkflow_id(String.valueOf(workflowId));
            request.setStoryboard_ids(storyboardIds);
            Optional.ofNullable(workflowTask.getParams().get(WorkflowTaskConstant.VIDEO_VOICE_TYPE)).ifPresentOrElse(request::setVoice_type, () -> request.setVoice_type("磁性男声"));
            Optional.ofNullable(workflowTask.getParams().get(WorkflowTaskConstant.VIDEO_BGM_TYPE)).ifPresentOrElse(request::setBgm_type, () -> request.setBgm_type("摇滚质感"));
            log.info("[FullVideoGenAlgoTaskProcessor.generateTasks] 构建请求: {}", request);
            AlgoResponse response = algoGateway.createVideoMergeTask(request);
            log.info("[FullVideoGenAlgoTaskProcessor.generateTasks] 算法服务响应: {}", response);
            AlgoTaskBO algoTaskBO = new AlgoTaskBO();
            algoTaskBO.setAlgoTaskId(response.getTaskId());
            algoTaskBO.setRelevantId(workflowId);
            algoTaskBO.setRelevantIdType(RelevanceType.WORKFLOW_ID);
            log.info("[FullVideoGenAlgoTaskProcessor.generateTasks] 生成的任务: {}", algoTaskBO);
            return Collections.singletonList(algoTaskBO);
        } catch (Exception e) {
            log.error("[FullVideoGenAlgoTaskProcessor.generateTasks] Failed to generate script task for workflow: " + workflowId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask) {
        log.info("[FullVideoGenAlgoTaskProcessor.singleTaskSuccessPostProcess] 开始处理完整视频, algoTaskId: {}", algoTask.getAlgoTaskId());
        try {
            // 获取完整视频结果，添加重试逻辑
            String taskId = Objects.toString(algoTask.getAlgoTaskId());
            MultipartFile videoResult = retryGetResultOperation(
                () -> algoGateway.getVideoMergeResult(taskId),
                "getVideoMergeResult",
                taskId
            );
            
            log.info("[FullVideoGenAlgoTaskProcessor.singleTaskSuccessPostProcess] 获取到完整视频: {}", videoResult);
            if (videoResult == null) {
                log.error("[FullVideoGenAlgoTaskProcessor.singleTaskSuccessPostProcess] 获取完整视频结果失败, algoTaskId: {}", algoTask.getAlgoTaskId());
                return;
            }
            FicWorkflowTaskBO workflowTask = ficWorkflowTaskRepository.findById(algoTask.getWorkflowTaskId());
            if (workflowTask == null) {
                log.error("[FullVideoGenAlgoTaskProcessor.singleTaskSuccessPostProcess] 工作流任务不存在, workflowTaskId: {}", algoTask.getWorkflowTaskId());
                return;
            }
            Long workflowId = workflowTask.getWorkflowId();
            String fileName = String.format("full_video_%s_%s_%s", workflowId, algoTask.getAlgoTaskId(), videoResult.getName());
            log.info("[FullVideoGenAlgoTaskProcessor.singleTaskSuccessPostProcess] 上传文件名: {}", fileName);
            Resp<String> uploadResp = fileGateway.saveFile(fileName, videoResult);
            if (!uploadResp.isSuccess()) {
                log.error("[FullVideoGenAlgoTaskProcessor.singleTaskSuccessPostProcess] 上传完整视频到OSS失败, algoTaskId: {}, error: {}", algoTask.getAlgoTaskId(), uploadResp.getMessage());
                return;
            }
            List<FicResourceBO> oldFullVideoResources = ficResourceRepository.findValidByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.FULL_VIDEO);
            for (FicResourceBO oldFullVideoResource : oldFullVideoResources) {
                ficResourceRepository.offlineResourceById(oldFullVideoResource.getId());
                log.info("[FullVideoGenAlgoTaskProcessor.singleTaskSuccessPostProcess] 下线旧资源, resourceId: {}", oldFullVideoResource.getId());
            }
            FicResourceBO ficResourceBO = new FicResourceBO();
            ficResourceBO.setWorkflowId(workflowTask.getWorkflowId());
            ficResourceBO.setResourceType(ResourceTypeEnum.FULL_VIDEO.name());
            ficResourceBO.setResourceUrl(uploadResp.getData());
            ficResourceBO.setRelevanceId(algoTask.getRelevantId());
            ficResourceBO.setRelevanceType(algoTask.getRelevantIdType());
            ficResourceBO.setResourceStorageType(ResourceStorageTypeEnum.ALI_CLOUD_OSS.name());
            ficResourceBO.setStatus(CommonStatusEnum.VALID.getValue());
            ficResourceBO.setGmtCreate(System.currentTimeMillis());
            ficResourceBO.setOriginName(videoResult.getOriginalFilename());
            ficResourceRepository.insert(ficResourceBO);
            log.info("[FullVideoGenAlgoTaskProcessor.singleTaskSuccessPostProcess] 完整视频处理完成, algoTaskId: {}, resourceId: {}", algoTask.getAlgoTaskId(), ficResourceBO.getId());
        } catch (Exception e) {
            log.error("[FullVideoGenAlgoTaskProcessor.singleTaskSuccessPostProcess] 处理完整视频失败, algoTaskId: {}", algoTask.getAlgoTaskId(), e);
        }
    }

}
