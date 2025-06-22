package com.taichu.application.service.inner.algo.v2;

import com.taichu.application.service.inner.algo.v2.context.AlgoTaskContext;
import com.taichu.application.service.inner.algo.v2.context.StoryboardImgTaskContext;
import com.taichu.common.common.model.Resp;
import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.algo.model.request.StoryboardImageRequest;
import com.taichu.domain.enums.*;
import com.taichu.domain.model.*;
import com.taichu.infra.repo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class StoryboardImgAlgoTaskProcessorV2 extends AbstractAlgoTaskProcessorV2 {
    protected final FicStoryboardRepository ficStoryboardRepository;
    protected final AlgoGateway algoGateway;
    protected final FicRoleRepository ficRoleRepository;
    protected final FileGateway fileGateway;
    protected final FicResourceRepository ficResourceRepository;

    public StoryboardImgAlgoTaskProcessorV2(FicStoryboardRepository ficStoryboardRepository, 
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
        return AlgoTaskTypeEnum.STORYBOARD_IMG_GENERATION;
    }

    @Override
    public List<AlgoTaskContext> createTaskContextList(FicWorkflowTaskBO workflowTask) {
        Long workflowId = workflowTask.getWorkflowId();
        log.info("[StoryboardImgAlgoTaskProcessorV2.createTaskContextList] 开始创建分镜图片任务上下文, workflowId: {}", workflowId);
        
        // 查询分镜
        List<FicStoryboardBO> ficStoryboardBOList = ficStoryboardRepository.findValidByWorkflowId(workflowId);
        if (ficStoryboardBOList.isEmpty()) {
            log.warn("[StoryboardImgAlgoTaskProcessorV2.createTaskContextList] 分镜为空, workflowId: {}", workflowId);
            return List.of();
        }
        
        // 查询角色列表
        List<FicRoleBO> ficRoleBOList = ficRoleRepository.findByWorkflowId(workflowId);
        if (CollectionUtils.isEmpty(ficRoleBOList)) {
            log.warn("[StoryboardImgAlgoTaskProcessorV2.createTaskContextList] 角色列表为空, workflowId: {}", workflowId);
            return List.of();
        }
        
        List<AlgoTaskContext> contexts = new ArrayList<>();
        for (FicStoryboardBO ficStoryboardBO : ficStoryboardBOList) {
            StoryboardImgTaskContext context = new StoryboardImgTaskContext();
            context.setWorkflowId(workflowId);
            context.setWorkflowTaskId(workflowTask.getId());
            context.setStoryboard(ficStoryboardBO);
            context.setRoles(ficRoleBOList);
            
            // 从工作流任务参数中获取配置
            Optional.ofNullable(workflowTask.getParams().get(WorkflowTaskConstant.IMG_IMAGE_STYLE))
                .ifPresent(context::setImageStyle);
            Optional.ofNullable(workflowTask.getParams().get(WorkflowTaskConstant.IMG_SCALE))
                .map(Float::parseFloat)
                .ifPresent(context::setScale);
            Optional.ofNullable(workflowTask.getParams().get(WorkflowTaskConstant.IMG_STYLE_SCALE))
                .map(Float::parseFloat)
                .ifPresent(context::setStyleScale);
            
            contexts.add(context);
        }
        
        return contexts;
    }

    @Override
    public void validateContext(AlgoTaskContext context) {
        StoryboardImgTaskContext storyboardContext = (StoryboardImgTaskContext) context;
        if (storyboardContext.getStoryboard() == null) {
            throw new IllegalArgumentException("分镜信息不能为空");
        }
        if (storyboardContext.getRoles() == null || storyboardContext.getRoles().isEmpty()) {
            throw new IllegalArgumentException("角色列表不能为空");
        }
        if (storyboardContext.getWorkflowId() == null) {
            throw new IllegalArgumentException("工作流ID不能为空");
        }
    }

    @Override
    public AlgoTaskBOV2 generateAlgoTask(AlgoTaskContext context) {
        StoryboardImgTaskContext storyboardContext = (StoryboardImgTaskContext) context;
        
        // 将FicRoleBO列表转换为RoleDTO列表
        List<StoryboardImageRequest.RoleDTO> roleDTOList = storyboardContext.getRoles().stream()
                .map(roleBO -> {
                    StoryboardImageRequest.RoleDTO roleDTO = new StoryboardImageRequest.RoleDTO();
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
        StoryboardImageRequest request = new StoryboardImageRequest();
        request.setWorkflow_id(String.valueOf(storyboardContext.getWorkflowId()));
        request.setStoryboard_id(String.valueOf(storyboardContext.getStoryboard().getId()));
        request.setRoles(roleDTOList);
        request.setStoryboard(storyboardContext.getStoryboard().getContent());
        request.setImage_style(storyboardContext.getImageStyle());
        request.setScale(storyboardContext.getScale());
        request.setStyle_scale(storyboardContext.getStyleScale());

        // 调用算法服务（使用基类的重试机制）
        String operationName = "Create storyboard image task for storyboard: " + storyboardContext.getStoryboard().getId();
        AlgoResponse response = callAlgoServiceWithRetry(operationName, () -> algoGateway.createStoryboardImageTask(request));
        
        if (response == null) {
            throw new RuntimeException("创建分镜图片任务失败");
        }
        
        // 创建AlgoTaskBO
        AlgoTaskBOV2 algoTaskBO = new AlgoTaskBOV2();
        algoTaskBO.setAlgoTaskId(response.getTaskId());
        algoTaskBO.setRelevantId(storyboardContext.getStoryboard().getId());
        algoTaskBO.setRelevantIdType(RelevanceType.STORYBOARD_ID);
        algoTaskBO.setTaskSummary(storyboardContext.getTaskSummary());
        
        return algoTaskBO;
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask, AlgoTaskContext context) throws Exception {
        StoryboardImgTaskContext storyboardContext = (StoryboardImgTaskContext) context;
        log.info("[StoryboardImgAlgoTaskProcessorV2.singleTaskSuccessPostProcess] 开始处理分镜图片, algoTaskId: {}", algoTask.getAlgoTaskId());
        
        // 获取分镜图片结果（使用基类的重试机制）
        String taskId = Objects.toString(algoTask.getAlgoTaskId());
        MultipartFile storyboardImgResult = retryGetResultOperation(
            () -> algoGateway.getStoryboardImageResult(taskId),
            "getStoryboardImageResult",
            taskId
        );
        
        if (storyboardImgResult == null) {
            throw new Exception("获取分镜图片结果失败");
        }
        
        Long workflowId = storyboardContext.getWorkflowId();
        String fileName = String.format("storyboard_img_%s_%s_%s", workflowId, algoTask.getAlgoTaskId(), storyboardImgResult.getOriginalFilename());
        
        // 上传到OSS
        Resp<String> uploadResp = fileGateway.saveFile(fileName, storyboardImgResult);
        if (!uploadResp.isSuccess()) {
            throw new Exception("上传分镜图片到OSS失败");
        }
        
        // 清理关联storyboard已存在的图片资源
        cleanupStoryboardImgResources(workflowId, algoTask.getRelevantId(), algoTask.getRelevantIdType());
        
        // 保存新的资源记录
        FicResourceBO ficResourceBO = new FicResourceBO();
        ficResourceBO.setWorkflowId(workflowId);
        ficResourceBO.setResourceType(ResourceTypeEnum.STORYBOARD_IMG.name());
        ficResourceBO.setResourceUrl(uploadResp.getData());
        ficResourceBO.setRelevanceId(algoTask.getRelevantId());
        ficResourceBO.setRelevanceType(algoTask.getRelevantIdType());
        ficResourceBO.setResourceStorageType(ResourceStorageTypeEnum.ALI_CLOUD_OSS.name());
        ficResourceBO.setStatus(CommonStatusEnum.VALID.getValue());
        ficResourceBO.setGmtCreate(System.currentTimeMillis());
        ficResourceBO.setOriginName(storyboardImgResult.getOriginalFilename());
        ficResourceRepository.insert(ficResourceBO);
        
        log.info("[StoryboardImgAlgoTaskProcessorV2.singleTaskSuccessPostProcess] 分镜图片处理完成, algoTaskId: {}, resourceId: {}", algoTask.getAlgoTaskId(), ficResourceBO.getId());
    }

    @Override
    public void singleTaskFailedPostProcess(FicAlgoTaskBO algoTask, AlgoTaskContext context, Exception e) {
        log.error("[StoryboardImgAlgoTaskProcessorV2.singleTaskFailedPostProcess] 分镜图片生成任务失败: {}", 
            algoTask.buildSummary(), e);
        
        // 清理关联storyboard已存在的图片资源
        try {
            StoryboardImgTaskContext storyboardContext = (StoryboardImgTaskContext) context;
            cleanupStoryboardImgResources(storyboardContext.getWorkflowId(), algoTask.getRelevantId(), algoTask.getRelevantIdType());
        } catch (Exception cleanupException) {
            log.error("[StoryboardImgAlgoTaskProcessorV2.singleTaskFailedPostProcess] 清理失败任务资源时发生异常: {}", 
                algoTask.buildSummary(), cleanupException);
        }
    }

    @Override
    public void postProcessAllComplete(FicWorkflowTaskBO workflowTask, List<AlgoTaskContext> contexts) {
        log.info("[StoryboardImgAlgoTaskProcessorV2.postProcessAllComplete] 所有分镜图片生成任务完成, workflowId: {}", workflowTask.getWorkflowId());
    }

    @Override
    public void postProcessAnyFailed(FicWorkflowTaskBO workflowTask, List<AlgoTaskContext> contexts) {
        super.postProcessAnyFailed(workflowTask, contexts);
        Long workflowId = workflowTask.getWorkflowId();
        cleanupFailedStoryboardImgTask(workflowId);
    }

    private void cleanupFailedStoryboardImgTask(Long workflowId) {
        // 清理已创建的分镜图片资源
        try {
            List<FicResourceBO> storyboardImages = ficResourceRepository.findByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.STORYBOARD_IMG);
            for (FicResourceBO resource : storyboardImages) {
                ficResourceRepository.offlineResourceById(resource.getId());
            }
            log.info("[cleanupFailedStoryboardImgTask] 清理失败任务资源成功, workflowId: {}", workflowId);
        } catch (Exception e) {
            log.error("[cleanupFailedStoryboardImgTask] 清理失败任务资源失败, workflowId: {}", workflowId, e);
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

    /**
     * 清理关联storyboard的图片资源
     * @param workflowId 工作流ID
     * @param relevantId 关联ID（storyboard ID）
     * @param relevantType 关联类型
     */
    private void cleanupStoryboardImgResources(Long workflowId, Long relevantId, String relevantType) {
        try {
            List<FicResourceBO> oldResources = ficResourceRepository.findByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.STORYBOARD_IMG);
            for (FicResourceBO resource : oldResources) {
                if (Objects.equals(resource.getRelevanceId(), relevantId) && 
                    Objects.equals(resource.getRelevanceType(), relevantType)) {
                    ficResourceRepository.offlineResourceById(resource.getId());
                    log.info("[cleanupStoryboardImgResources] 下线旧资源, resourceId: {}, storyboardId: {}", resource.getId(), relevantId);
                }
            }
        } catch (Exception e) {
            log.error("[cleanupStoryboardImgResources] 清理storyboard图片资源失败, workflowId: {}, storyboardId: {}", workflowId, relevantId, e);
        }
    }
} 
