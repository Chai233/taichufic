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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    public List<AlgoResponse> generateTasks(FicWorkflowTaskBO workflowTask) {
        try {
            List<FicStoryboardBO> storyboardBOS = ficStoryboardRepository.findByWorkflowId(workflowTask.getWorkflowId());
            List<String> storyboardIds = StreamUtil.toStream(storyboardBOS)
                    .filter(Objects::nonNull)
                    .map(FicStoryboardBO::getId)
                    .map(Object::toString)
                    .collect(Collectors.toList());

            if (storyboardIds.isEmpty()) {
                log.error("故事板ID列表为空, workflowId: {}", workflowTask.getWorkflowId());
                return new ArrayList<>();
            }

            // 构建剧本生成请求
            VideoMergeRequest request = new VideoMergeRequest();
            request.setWorkflow_id(String.valueOf(workflowTask.getWorkflowId()));
            request.setStoryboard_ids(storyboardIds);
            request.setVoice_type(VoiceTypeEnum.DEFAULT_MAN_SOUND.getValue());
            request.setBgm_type(null);

            // 调用算法服务生成剧本
            AlgoResponse response = algoGateway.createVideoMergeTask(request);
            
            // 返回响应列表
            List<AlgoResponse> responses = new ArrayList<>();
            responses.add(response);
            return responses;
        } catch (Exception e) {
            log.error("Failed to generate script task for workflow: " + workflowTask.getWorkflowId(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask) {
        try {
            // 从算法服务获取生成的视频
            MultipartFile videoResult = algoGateway.getVideoMergeResult(Objects.toString(algoTask.getAlgoTaskId()));
            if (videoResult == null) {
                log.error("获取完整视频结果失败, algoTaskId: {}", algoTask.getAlgoTaskId());
                return;
            }

            // 获取工作流ID
            FicWorkflowTaskBO workflowTask = ficWorkflowTaskRepository.findById(algoTask.getWorkflowTaskId());
            if (workflowTask == null) {
                log.error("工作流任务不存在, workflowTaskId: {}", algoTask.getWorkflowTaskId());
                return;
            }
            Long workflowId = workflowTask.getWorkflowId();

            // 构建文件名
            String fileName = String.format("full_video_%s_%s_%s",
                workflowId,
                algoTask.getAlgoTaskId(),
                videoResult.getName());

            // 上传到 OSS
            Resp<String> uploadResp = fileGateway.saveFile(fileName, videoResult);
            if (!uploadResp.isSuccess()) {
                log.error("上传完整视频到OSS失败, algoTaskId: {}, error: {}",
                    algoTask.getAlgoTaskId(), uploadResp.getMessage());
                return;
            }

            // 删除旧的资源
            List<FicResourceBO> oldFullVideoResources = ficResourceRepository.findByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.FULL_VIDEO);
            for (FicResourceBO oldFullVideoResource : oldFullVideoResources) {
                ficResourceRepository.offlineResourceById(oldFullVideoResource.getId());
            }

            // 保存资源信息
            FicResourceBO ficResourceBO = new FicResourceBO();
            ficResourceBO.setWorkflowId(workflowTask.getWorkflowId());
            ficResourceBO.setResourceType(FicResourceTypeEnum.FULL_VIDEO.getValue());
            ficResourceBO.setResourceUrl(uploadResp.getData());
            ficResourceBO.setRelevanceId(algoTask.getRelevantId());
            ficResourceBO.setRelevanceType(algoTask.getRelevantIdType());
            ficResourceBO.setResourceStorageType(ResourceStorageTypeEnum.ALI_CLOUD_OSS.name());
            ficResourceBO.setStatus(CommonStatusEnum.VALID.getValue());
            ficResourceBO.setGmtCreate(System.currentTimeMillis());
            ficResourceBO.setOriginName(videoResult.getOriginalFilename());

            // 保存到数据库
            ficResourceRepository.insert(ficResourceBO);

            log.info("完整视频处理完成, algoTaskId: {}, resourceId: {}",
                algoTask.getAlgoTaskId(), ficResourceBO.getId());
        } catch (Exception e) {
            log.error("处理完整视频失败, algoTaskId: {}", algoTask.getAlgoTaskId(), e);
        }
    }

}
