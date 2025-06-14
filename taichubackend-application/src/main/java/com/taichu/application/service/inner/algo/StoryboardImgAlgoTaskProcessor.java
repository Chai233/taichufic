package com.taichu.application.service.inner.algo;

import com.taichu.common.common.model.Resp;
import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.algo.model.request.StoryboardImageRequest;
import com.taichu.domain.enums.*;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicResourceBO;
import com.taichu.domain.model.FicRoleBO;
import com.taichu.domain.model.FicStoryboardBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.domain.model.AlgoTaskStatus;
import com.taichu.infra.repo.*;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class StoryboardImgAlgoTaskProcessor extends AbstractAlgoTaskProcessor {
    private final FicStoryboardRepository ficStoryboardRepository;
    private final AlgoGateway algoGateway;
    private final FicRoleRepository ficRoleRepository;
    private final FileGateway fileGateway;
    private final FicResourceRepository ficResourceRepository;

    public StoryboardImgAlgoTaskProcessor(FicStoryboardRepository ficStoryboardRepository, AlgoGateway algoGateway, FicRoleRepository ficRoleRepository, FileGateway fileGateway, FicResourceRepository ficResourceRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository, FicWorkflowRepository ficWorkflowRepository) {
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
    public List<AlgoResponse> generateTasks(FicWorkflowTaskBO workflowTask) {
        Long workflowId = workflowTask.getWorkflowId();

        // 1. 查询分镜
        List<FicStoryboardBO> ficStoryboardBOList = ficStoryboardRepository.findByWorkflowId(workflowId);
        if (ficStoryboardBOList.isEmpty()) {
            return List.of();
        }

        List<AlgoResponse> algoResponseList = new ArrayList<>(ficStoryboardBOList.size());
        for (FicStoryboardBO ficStoryboardBO : ficStoryboardBOList) {
            // 调用算法服务
            String operationName = "Call algorithm service for workflow: " + workflowId;
            AlgoResponse response = callAlgoServiceWithRetry(operationName, () -> callAlgoServiceGenStoryboardImg(workflowTask, workflowId, ficStoryboardBO.getId()));

            // 检查算法服务响应
            if (response == null) {
                log.error("Algorithm service failed to create script task for workflow: {}, after {} retries",
                        workflowId, getMaxRetry());
                return Collections.emptyList();
            }

            // 添加到返回列表
            algoResponseList.add(response);
        }

        return algoResponseList;
    }

    /**
     * 调用算法服务生成分镜图片
     *
     * @param workflowId   工作流ID
     * @param storyboardId 分镜ID
     * @return 算法服务响应结果, 如果分镜不存在或角色列表为空则返回null
     */
    protected AlgoResponse callAlgoServiceGenStoryboardImg(FicWorkflowTaskBO workflowTask, Long workflowId, Long storyboardId) {
        // 查询分镜信息
        FicStoryboardBO ficStoryboardBO = ficStoryboardRepository.findById(storyboardId);
        if (ficStoryboardBO == null) {
            log.error("分镜不存在, storyboardId: {}", storyboardId);
            return null;
        }

        // 查询角色列表
        List<FicRoleBO> ficRoleBOList = ficRoleRepository.findByWorkflowId(workflowId);
        if (CollectionUtils.isEmpty(ficRoleBOList)) {
            log.warn("角色列表为空, workflowId: {}", workflowId);
            return null;
        }

        // 将FicRoleBO列表转换为RoleDTO列表
        List<StoryboardImageRequest.RoleDTO> roleDTOList = ficRoleBOList.stream()
                .map(roleBO -> {
                    StoryboardImageRequest.RoleDTO roleDTO = new StoryboardImageRequest.RoleDTO();
                    roleDTO.setRole(roleBO.getRoleName());

                    Long defaultImageResourceId = roleBO.getDefaultImageResourceId();
                    FicResourceBO ficResourceBO = ficResourceRepository.findById(defaultImageResourceId);
                    roleDTO.setImage(ficResourceBO.getOriginName());
                    return roleDTO;
                })
                .collect(Collectors.toList());

        // 构建请求参数并调用算法服务
        StoryboardImageRequest request = new StoryboardImageRequest();
        request.setWorkflow_id(String.valueOf(workflowId));
        request.setStoryboard_id(String.valueOf(storyboardId));
        request.setRoles(roleDTOList);
        request.setStoryboard(ficStoryboardBO.getContent());

        Optional.ofNullable(workflowTask.getParams().get(WorkflowTaskConstant.IMG_IMAGE_STYLE)).ifPresent(request::setImage_style);
        Optional.ofNullable(workflowTask.getParams().get(WorkflowTaskConstant.IMG_SCALE))
                .map(Float::parseFloat)
                .ifPresent(request::setScale);
        Optional.ofNullable(workflowTask.getParams().get(WorkflowTaskConstant.IMG_STYLE_SCALE))
                .map(Float::parseFloat)
                .ifPresent(request::setStyle_scale);
    
        return algoGateway.createStoryboardImageTask(request);
    }

    @Override
    public TaskStatusEnum checkSingleTaskStatus(FicAlgoTaskBO algoTask) {
        AlgoTaskStatus taskStatus = algoGateway.checkTaskStatus(Objects.toString(algoTask.getAlgoTaskId()));
        if (taskStatus.isCompleted()) {
            return TaskStatusEnum.COMPLETED;
        } else if (taskStatus.isRunning()) {
            return TaskStatusEnum.RUNNING;
        }

        return TaskStatusEnum.FAILED;
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask) {
        /*
         * 下载图片，上传OSS
         */
        MultipartFile storyboardImgResult = algoGateway.getStoryboardImageResult(Objects.toString(algoTask.getAlgoTaskId()));
        if (storyboardImgResult == null) {
            log.error("获取分镜图片结果失败, algoTaskId: {}", algoTask.getAlgoTaskId());
            return;
        }

        try {
            // 获取工作流ID
            FicWorkflowTaskBO workflowTask = ficWorkflowTaskRepository.findById(algoTask.getWorkflowTaskId());
            if (workflowTask == null) {
                log.error("工作流任务不存在, workflowTaskId: {}", algoTask.getWorkflowTaskId());
                return;
            }
            Long workflowId = workflowTask.getWorkflowId();

            // 构建文件名
            String fileName = String.format("storyboard_img_%s_%s_%s",
                    workflowId,
                    algoTask.getAlgoTaskId(),
                    storyboardImgResult.getName());

            // 上传到 OSS
            Resp<String> uploadResp = fileGateway.saveFile(fileName, storyboardImgResult);
            if (!uploadResp.isSuccess()) {
                log.error("上传分镜图片到OSS失败, algoTaskId: {}, error: {}",
                        algoTask.getAlgoTaskId(), uploadResp.getMessage());
                return;
            }

            // 删除旧的资源
            List<FicResourceBO> oldFullVideoResources = ficResourceRepository.findByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.STORYBOARD_IMG);
            for (FicResourceBO resource : oldFullVideoResources) {
                if (Objects.equals(resource.getRelevanceId(), algoTask.getRelevantId())
                        && Objects.equals(resource.getResourceType(), algoTask.getRelevantIdType())
                ) {
                    ficResourceRepository.offlineResourceById(resource.getId());
                    break;
                }
            }

            // 保存资源信息
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

            // 保存到数据库
            ficResourceRepository.insert(ficResourceBO);

            log.info("分镜图片处理完成, algoTaskId: {}, resourceId: {}",
                    algoTask.getAlgoTaskId(), ficResourceBO.getId());
        } catch (Exception e) {
            log.error("处理分镜图片失败, algoTaskId: {}", algoTask.getAlgoTaskId(), e);
        }
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected AlgoGateway getAlgoGateway() {
        return algoGateway;
    }

}
