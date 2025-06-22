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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SingleStoryboardImgAlgoTaskProcessorV2 extends AbstractAlgoTaskProcessorV2 {
    private final FicStoryboardRepository ficStoryboardRepository;
    private final AlgoGateway algoGateway;
    private final FicRoleRepository ficRoleRepository;
    private final FileGateway fileGateway;
    private final FicResourceRepository ficResourceRepository;

    public SingleStoryboardImgAlgoTaskProcessorV2(FicStoryboardRepository ficStoryboardRepository,
                                                 FicRoleRepository ficRoleRepository,
                                                 AlgoGateway algoGateway,
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
        String operationName = "Create single storyboard image task for storyboard: " + storyboardContext.getStoryboard().getId();
        AlgoResponse response = callAlgoServiceWithRetry(operationName, () -> algoGateway.createStoryboardImageTask(request));
        
        if (response == null) {
            throw new RuntimeException("创建单分镜图片任务失败");
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
        // 处理单分镜图片生成成功后的逻辑
        log.info("[SingleStoryboardImgAlgoTaskProcessorV2.singleTaskSuccessPostProcess] 单分镜图片生成成功, algoTaskId: {}", algoTask.getAlgoTaskId());
        
        // 获取分镜图片结果，添加重试逻辑
        String taskId = Objects.toString(algoTask.getAlgoTaskId());
        MultipartFile storyboardImgResult = retryGetResultOperation(
            () -> algoGateway.getStoryboardImageResult(taskId),
            "getStoryboardImageResult",
            taskId
        );
        
        if (storyboardImgResult == null) {
            throw new Exception("获取分镜图片结果失败, algoTaskId: " + algoTask.getAlgoTaskId());
        }
        
        // 处理图片上传和资源保存逻辑
        processStoryboardImageResult(algoTask, storyboardImgResult);
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

    /**
     * 处理分镜图片结果
     */
    private void processStoryboardImageResult(FicAlgoTaskBO algoTask, MultipartFile storyboardImgResult) throws Exception {
        FicWorkflowTaskBO workflowTask = ficWorkflowTaskRepository.findById(algoTask.getWorkflowTaskId());
        if (workflowTask == null) {
            throw new Exception("工作流任务不存在, workflowTaskId: " + algoTask.getWorkflowTaskId());
        }
        
        Long workflowId = workflowTask.getWorkflowId();
        String fileName = String.format("storyboard_img_%s_%s_%s", workflowId, algoTask.getAlgoTaskId(), storyboardImgResult.getOriginalFilename());
        log.info("[SingleStoryboardImgAlgoTaskProcessorV2.processStoryboardImageResult] 上传文件名: {}", fileName);
        
        Resp<String> uploadResp = fileGateway.saveFile(fileName, storyboardImgResult);
        if (!uploadResp.isSuccess()) {
            throw new Exception("上传分镜图片到OSS失败, algoTaskId: " + algoTask.getAlgoTaskId() + ", error: " + uploadResp.getMessage());
        }
        
        // 删除旧的资源
        List<FicResourceBO> oldResources = ficResourceRepository.findByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.STORYBOARD_IMG);
        for (FicResourceBO resource : oldResources) {
            if (Objects.equals(resource.getRelevanceId(), algoTask.getRelevantId()) && 
                Objects.equals(resource.getRelevanceType(), algoTask.getRelevantIdType().toString())) {
                ficResourceRepository.offlineResourceById(resource.getId());
                log.info("[SingleStoryboardImgAlgoTaskProcessorV2.processStoryboardImageResult] 下线旧资源, resourceId: {}", resource.getId());
                break;
            }
        }
        
        // 保存新资源
        FicResourceBO ficResourceBO = new FicResourceBO();
        ficResourceBO.setWorkflowId(workflowId);
        ficResourceBO.setResourceType(ResourceTypeEnum.STORYBOARD_IMG.name());
        ficResourceBO.setResourceUrl(uploadResp.getData());
        ficResourceBO.setRelevanceId(algoTask.getRelevantId());
        ficResourceBO.setRelevanceType(algoTask.getRelevantIdType().toString());
        ficResourceBO.setResourceStorageType(ResourceStorageTypeEnum.ALI_CLOUD_OSS.name());
        ficResourceBO.setStatus(CommonStatusEnum.VALID.getValue());
        ficResourceBO.setGmtCreate(System.currentTimeMillis());
        ficResourceBO.setOriginName(storyboardImgResult.getOriginalFilename());
        ficResourceRepository.insert(ficResourceBO);
        
        log.info("[SingleStoryboardImgAlgoTaskProcessorV2.processStoryboardImageResult] 分镜图片处理完成, algoTaskId: {}, resourceId: {}", 
            algoTask.getAlgoTaskId(), ficResourceBO.getId());
    }
} 