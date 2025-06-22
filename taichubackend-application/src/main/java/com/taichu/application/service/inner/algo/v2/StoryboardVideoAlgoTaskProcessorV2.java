package com.taichu.application.service.inner.algo.v2;

import com.taichu.application.service.inner.algo.v2.context.AlgoTaskContext;
import com.taichu.application.service.inner.algo.v2.context.StoryboardVideoTaskContext;
import com.taichu.application.service.inner.algo.ImageVideoStyleEnum;
import com.taichu.common.common.model.Resp;
import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.algo.model.request.StoryboardVideoRequest;
import com.taichu.domain.enums.*;
import com.taichu.domain.model.*;
import com.taichu.infra.repo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class StoryboardVideoAlgoTaskProcessorV2 extends AbstractAlgoTaskProcessorV2 {
    private final FicStoryboardRepository ficStoryboardRepository;
    private final AlgoGateway algoGateway;
    private final FicRoleRepository ficRoleRepository;
    private final FileGateway fileGateway;
    private final FicResourceRepository ficResourceRepository;

    public StoryboardVideoAlgoTaskProcessorV2(FicStoryboardRepository ficStoryboardRepository, 
                                             AlgoGateway algoGateway, 
                                             FicRoleRepository ficRoleRepository, 
                                             FileGateway fileGateway, 
                                             FicResourceRepository ficResourceRepository,
                                             FicWorkflowTaskRepository ficWorkflowTaskRepository,
                                             FicWorkflowRepository ficWorkflowRepository) {
        super(ficWorkflowTaskRepository, ficWorkflowRepository);
        this.ficStoryboardRepository = ficStoryboardRepository;
        this.algoGateway = algoGateway;
        this.ficRoleRepository = ficRoleRepository;
        this.fileGateway = fileGateway;
        this.ficResourceRepository = ficResourceRepository;
    }

    @Override
    public AlgoTaskTypeEnum getAlgoTaskType() {
        return AlgoTaskTypeEnum.STORYBOARD_VIDEO_GENERATION;
    }

    @Override
    public List<AlgoTaskContext> createTaskContextList(FicWorkflowTaskBO workflowTask) {
        Long workflowId = workflowTask.getWorkflowId();
        log.info("[StoryboardVideoAlgoTaskProcessorV2.createTaskContextList] 开始创建分镜视频任务上下文, workflowId: {}", workflowId);
        
        List<FicStoryboardBO> ficStoryboardBOList = ficStoryboardRepository.findValidByWorkflowId(workflowId);
        if (ficStoryboardBOList.isEmpty()) {
            log.warn("[StoryboardVideoAlgoTaskProcessorV2.createTaskContextList] 分镜为空, workflowId: {}", workflowId);
            return List.of();
        }
        
        // 查询角色列表
        List<FicRoleBO> ficRoleBOList = ficRoleRepository.findByWorkflowId(workflowId);
        if (CollectionUtils.isEmpty(ficRoleBOList)) {
            log.warn("[StoryboardVideoAlgoTaskProcessorV2.createTaskContextList] 角色列表为空, workflowId: {}", workflowId);
            return List.of();
        }
        
        List<AlgoTaskContext> contexts = new ArrayList<>();
        for (FicStoryboardBO ficStoryboardBO : ficStoryboardBOList) {
            StoryboardVideoTaskContext context = new StoryboardVideoTaskContext();
            context.setWorkflowId(workflowId);
            context.setWorkflowTaskId(workflowTask.getId());
            context.setStoryboard(ficStoryboardBO);
            context.setRoles(ficRoleBOList);
            
            // 从工作流任务参数中获取配置
            String voiceType = workflowTask.getParams().get(WorkflowTaskConstant.VIDEO_VOICE_TYPE);
            context.setVoiceType(StringUtils.isBlank(voiceType) ? 
                VoiceTypeEnum.DEFAULT_MAN_SOUND.getValue() : voiceType);
            
            String videoStyle = workflowTask.getParams().get(WorkflowTaskConstant.VIDEO_STYLE);
            context.setBgmType(StringUtils.isBlank(videoStyle) ? 
                ImageVideoStyleEnum.CYBER_PUNK.getValue() : videoStyle);
            
            contexts.add(context);
        }
        
        return contexts;
    }

    @Override
    public void validateContext(AlgoTaskContext context) {
        StoryboardVideoTaskContext videoContext = (StoryboardVideoTaskContext) context;
        if (videoContext.getStoryboard() == null) {
            throw new IllegalArgumentException("分镜信息不能为空");
        }
        if (videoContext.getRoles() == null || videoContext.getRoles().isEmpty()) {
            throw new IllegalArgumentException("角色列表不能为空");
        }
        if (videoContext.getWorkflowId() == null) {
            throw new IllegalArgumentException("工作流ID不能为空");
        }
    }

    @Override
    public AlgoTaskBOV2 generateAlgoTask(AlgoTaskContext context) {
        StoryboardVideoTaskContext videoContext = (StoryboardVideoTaskContext) context;
        
        // 将FicRoleBO列表转换为RoleDTO列表
        List<StoryboardVideoRequest.RoleDTO> roleDTOList = videoContext.getRoles().stream()
                .map(roleBO -> {
                    StoryboardVideoRequest.RoleDTO roleDTO = new StoryboardVideoRequest.RoleDTO();
                    roleDTO.setRole(roleBO.getRoleName());

                    Long defaultImageResourceId = roleBO.getDefaultImageResourceId();
                    if (defaultImageResourceId != null) {
                        FicResourceBO ficResourceBO = ficResourceRepository.findById(defaultImageResourceId);
                        if (ficResourceBO != null) {
                            roleDTO.setImage(ficResourceBO.getOriginName());
                        }
                    }
                    return roleDTO;
                })
                .collect(Collectors.toList());

        // 构建请求参数
        StoryboardVideoRequest request = new StoryboardVideoRequest();
        request.setStoryboard_id(String.valueOf(videoContext.getStoryboard().getId()));
        request.setStoryboard(videoContext.getStoryboard().getContent());
        request.setWorkflow_id(String.valueOf(videoContext.getWorkflowId()));
        request.setRoles(roleDTOList);
        request.setVideo_style(videoContext.getBgmType());
        request.setVoice_type(videoContext.getVoiceType());

        // 调用算法服务（使用基类的重试机制）
        String operationName = "Create storyboard video task for storyboard: " + videoContext.getStoryboard().getId();
        AlgoResponse response = callAlgoServiceWithRetry(operationName, () -> algoGateway.createStoryboardVideoTask(request));
        
        if (response == null) {
            throw new RuntimeException("创建分镜视频任务失败");
        }
        
        // 创建AlgoTaskBO
        AlgoTaskBOV2 algoTaskBO = new AlgoTaskBOV2();
        algoTaskBO.setAlgoTaskId(response.getTaskId());
        algoTaskBO.setRelevantId(videoContext.getStoryboard().getId());
        algoTaskBO.setRelevantIdType(RelevanceType.STORYBOARD_ID);
        algoTaskBO.setTaskSummary(videoContext.getTaskSummary());
        
        return algoTaskBO;
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask, AlgoTaskContext context) throws Exception {
        StoryboardVideoTaskContext videoContext = (StoryboardVideoTaskContext) context;
        log.info("[StoryboardVideoAlgoTaskProcessorV2.singleTaskSuccessPostProcess] 开始处理分镜视频, algoTaskId: {}", algoTask.getAlgoTaskId());
        
        // 获取分镜视频结果（使用基类的重试机制）
        String taskId = Objects.toString(algoTask.getAlgoTaskId());
        MultipartFile videoResult = retryGetResultOperation(
            () -> algoGateway.getStoryboardVideoResult(taskId),
            "getStoryboardVideoResult",
            taskId
        );
        
        if (videoResult == null) {
            throw new Exception("获取分镜视频结果失败");
        }
        
        Long workflowId = videoContext.getWorkflowId();
        String fileName = String.format("storyboard_video_%s_%s_%s", workflowId, algoTask.getAlgoTaskId(), videoResult.getName());
        
        // 上传到OSS
        Resp<String> uploadResp = fileGateway.saveFile(fileName, videoResult);
        if (!uploadResp.isSuccess()) {
            throw new Exception("上传分镜视频到OSS失败");
        }
        
        // 删除旧的资源
        List<FicResourceBO> oldResources = ficResourceRepository.findByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.STORYBOARD_VIDEO);
        for (FicResourceBO resource : oldResources) {
            if (Objects.equals(resource.getRelevanceId(), algoTask.getRelevantId()) && 
                Objects.equals(resource.getRelevanceType(), algoTask.getRelevantIdType())) {
                ficResourceRepository.offlineResourceById(resource.getId());
                log.info("[StoryboardVideoAlgoTaskProcessorV2.singleTaskSuccessPostProcess] 下线旧资源, resourceId: {}", resource.getId());
                break;
            }
        }
        
        // 保存新的资源记录
        FicResourceBO ficResourceBO = new FicResourceBO();
        ficResourceBO.setWorkflowId(workflowId);
        ficResourceBO.setResourceType(ResourceTypeEnum.STORYBOARD_VIDEO.name());
        ficResourceBO.setResourceUrl(uploadResp.getData());
        ficResourceBO.setRelevanceId(algoTask.getRelevantId());
        ficResourceBO.setRelevanceType(algoTask.getRelevantIdType());
        ficResourceBO.setResourceStorageType(ResourceStorageTypeEnum.ALI_CLOUD_OSS.name());
        ficResourceBO.setStatus(CommonStatusEnum.VALID.getValue());
        ficResourceBO.setGmtCreate(System.currentTimeMillis());
        ficResourceBO.setOriginName(videoResult.getOriginalFilename());
        ficResourceRepository.insert(ficResourceBO);
        
        log.info("[StoryboardVideoAlgoTaskProcessorV2.singleTaskSuccessPostProcess] 分镜视频处理完成, algoTaskId: {}, resourceId: {}", algoTask.getAlgoTaskId(), ficResourceBO.getId());
    }

    @Override
    public void singleTaskFailedPostProcess(FicAlgoTaskBO algoTask, AlgoTaskContext context, Exception e) {
        log.error("[StoryboardVideoAlgoTaskProcessorV2.singleTaskFailedPostProcess] 分镜视频生成任务失败: {}", 
            algoTask.buildSummary(), e);
    }

    @Override
    public void postProcessAllComplete(FicWorkflowTaskBO workflowTask, List<AlgoTaskContext> contexts) {
        log.info("[StoryboardVideoAlgoTaskProcessorV2.postProcessAllComplete] 所有分镜视频生成任务完成, workflowId: {}", workflowTask.getWorkflowId());
    }

    @Override
    public void postProcessAnyFailed(FicWorkflowTaskBO workflowTask, List<AlgoTaskContext> contexts) {
        super.postProcessAnyFailed(workflowTask, contexts);
        Long workflowId = workflowTask.getWorkflowId();
        cleanupFailedStoryboardVideoTask(workflowId);
    }

    private void cleanupFailedStoryboardVideoTask(Long workflowId) {
        // 清理已创建的分镜视频资源
        try {
            List<FicResourceBO> storyboardVideos = ficResourceRepository.findByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.STORYBOARD_VIDEO);
            for (FicResourceBO resource : storyboardVideos) {
                ficResourceRepository.offlineResourceById(resource.getId());
            }
            log.info("[cleanupFailedStoryboardVideoTask] 清理失败任务资源成功, workflowId: {}", workflowId);
        } catch (Exception e) {
            log.error("[cleanupFailedStoryboardVideoTask] 清理失败任务资源失败, workflowId: {}", workflowId, e);
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