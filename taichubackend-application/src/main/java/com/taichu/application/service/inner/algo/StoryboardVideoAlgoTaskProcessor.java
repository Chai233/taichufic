package com.taichu.application.service.inner.algo;

import com.mysql.cj.util.StringUtils;
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
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class StoryboardVideoAlgoTaskProcessor extends AbstractAlgoTaskProcessor {
    private final FicStoryboardRepository ficStoryboardRepository;
    private final AlgoGateway algoGateway;
    private final FicRoleRepository ficRoleRepository;
    private final FileGateway fileGateway;
    private final FicResourceRepository ficResourceRepository;
    private final FicWorkflowTaskRepository ficWorkflowTaskRepository;

    protected StoryboardVideoAlgoTaskProcessor(FicWorkflowTaskRepository ficWorkflowTaskRepository, FicWorkflowRepository ficWorkflowRepository, FicStoryboardRepository ficStoryboardRepository, AlgoGateway algoGateway, FicRoleRepository ficRoleRepository, FileGateway fileGateway, FicResourceRepository ficResourceRepository) {
        super(ficWorkflowTaskRepository, ficWorkflowRepository);
        this.ficWorkflowTaskRepository = ficWorkflowTaskRepository;
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
    public List<AlgoTaskBO> generateTasks(FicWorkflowTaskBO workflowTask) {
        Long workflowId = workflowTask.getWorkflowId();
        log.info("[StoryboardVideoAlgoTaskProcessor.generateTasks] 开始生成分镜视频任务, workflowId: {}", workflowId);
        List<FicStoryboardBO> ficStoryboardBOList = ficStoryboardRepository.findValidByWorkflowId(workflowId);
        log.info("[StoryboardVideoAlgoTaskProcessor.generateTasks] 查询到分镜: {}", ficStoryboardBOList);
        if (ficStoryboardBOList.isEmpty()) {
            log.warn("[StoryboardVideoAlgoTaskProcessor.generateTasks] 分镜为空, workflowId: {}", workflowId);
            return List.of();
        }
        List<AlgoTaskBO> algoResponseList = new ArrayList<>(ficStoryboardBOList.size());
        for (FicStoryboardBO ficStoryboardBO : ficStoryboardBOList) {
            log.info("[StoryboardVideoAlgoTaskProcessor.generateTasks] 处理分镜, storyboardId: {}", ficStoryboardBO.getId());
            String operationName = "Call algorithm service for workflow: " + workflowId;
            AlgoResponse response = callAlgoServiceWithRetry(operationName, () -> callAlgoGenStoryboardVideo(workflowTask, ficStoryboardBO.getId()));
            log.info("[StoryboardVideoAlgoTaskProcessor.generateTasks] 算法服务响应: {}", response);
            if (response == null) {
                log.error("[StoryboardVideoAlgoTaskProcessor.generateTasks] Algorithm service failed to create storyboard_video task for workflow: {}, after {} retries", workflowId, getMaxRetry());
                return Collections.emptyList();
            }
            AlgoTaskBO algoTaskBO = new AlgoTaskBO();
            algoTaskBO.setAlgoTaskId(response.getTaskId());
            algoTaskBO.setRelevantId(ficStoryboardBO.getId());
            algoTaskBO.setRelevantIdType(RelevanceType.STORYBOARD_ID);
            algoResponseList.add(algoTaskBO);
            log.info("[StoryboardVideoAlgoTaskProcessor.generateTasks] 添加任务: {}", algoTaskBO);
        }
        return algoResponseList;
    }

    /**
     * 调用算法服务生成分镜视频
     *
     * @param workflowTask 工作流任务对象
     * @param storyboardId 分镜ID
     * @return 算法服务响应结果, 如果分镜不存在或角色列表为空则返回null
     */
    protected AlgoResponse callAlgoGenStoryboardVideo(FicWorkflowTaskBO workflowTask, Long storyboardId) {
        Long workflowId = workflowTask.getWorkflowId();
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
        List<StoryboardVideoRequest.RoleDTO> roleDTOList = ficRoleBOList.stream()
                .map(roleBO -> {
                    StoryboardVideoRequest.RoleDTO roleDTO = new StoryboardVideoRequest.RoleDTO();
                    roleDTO.setRole(roleBO.getRoleName());

                    Long defaultImageResourceId = roleBO.getDefaultImageResourceId();
                    FicResourceBO ficResourceBO = ficResourceRepository.findById(defaultImageResourceId);
                    roleDTO.setImage(ficResourceBO.getOriginName());
                    return roleDTO;
                })
                .collect(Collectors.toList());

        // 构建请求参数并调用算法服务
        String voiceType = workflowTask.getParams().get(WorkflowTaskConstant.VIDEO_VOICE_TYPE);
        voiceType = StringUtils.isEmptyOrWhitespaceOnly(voiceType) ? VoiceTypeEnum.DEFAULT_MAN_SOUND.getValue() : voiceType;
        String videoStyle = workflowTask.getParams().get(WorkflowTaskConstant.VIDEO_STYLE);
        videoStyle = StringUtils.isEmptyOrWhitespaceOnly(videoStyle) ? ImageVideoStyleEnum.CYBER_PUNK.getValue() : videoStyle;

        StoryboardVideoRequest request = new StoryboardVideoRequest();
        request.setWorkflow_id(String.valueOf(workflowId));
        request.setStoryboard_id(String.valueOf(storyboardId));
        request.setRoles(roleDTOList);
        request.setVoice_type(voiceType);
        request.setStoryboard(ficStoryboardBO.getContent());
        request.setVideo_style(videoStyle);
        return algoGateway.createStoryboardVideoTask(request);
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask) {
        log.info("[StoryboardVideoAlgoTaskProcessor.singleTaskSuccessPostProcess] 开始处理分镜视频, algoTaskId: {}", algoTask.getAlgoTaskId());
        
        // 获取分镜视频结果，添加重试逻辑
        String taskId = Objects.toString(algoTask.getAlgoTaskId());
        MultipartFile storyboardVideoResult = retryGetResultOperation(
            () -> algoGateway.getStoryboardVideoResult(taskId),
            "getStoryboardVideoResult",
            taskId
        );
        
        if (storyboardVideoResult == null) {
            log.error("[StoryboardVideoAlgoTaskProcessor.singleTaskSuccessPostProcess] 获取分镜视频结果失败, algoTaskId: {}", algoTask.getAlgoTaskId());
            return;
        }
        
        try {
            FicWorkflowTaskBO workflowTask = ficWorkflowTaskRepository.findById(algoTask.getWorkflowTaskId());
            if (workflowTask == null) {
                log.error("[StoryboardVideoAlgoTaskProcessor.singleTaskSuccessPostProcess] 工作流任务不存在, workflowTaskId: {}", algoTask.getWorkflowTaskId());
                return;
            }
            Long workflowId = workflowTask.getWorkflowId();
            String fileName = String.format("storyboard_video_%s_%s_%s", workflowId, algoTask.getAlgoTaskId(), storyboardVideoResult.getName());
            log.info("[StoryboardVideoAlgoTaskProcessor.singleTaskSuccessPostProcess] 上传文件名: {}", fileName);
            Resp<String> uploadResp = fileGateway.saveFile(fileName, storyboardVideoResult);
            if (!uploadResp.isSuccess()) {
                log.error("[StoryboardVideoAlgoTaskProcessor.singleTaskSuccessPostProcess] 上传分镜视频到OSS失败, algoTaskId: {}, error: {}", algoTask.getAlgoTaskId(), uploadResp.getMessage());
                return;
            }
            List<FicResourceBO> oldFullVideoResources = ficResourceRepository.findValidByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.STORYBOARD_VIDEO);
            for (FicResourceBO resource : oldFullVideoResources) {
                if (Objects.equals(resource.getRelevanceId(), algoTask.getRelevantId()) && Objects.equals(resource.getResourceType(), algoTask.getRelevantIdType())) {
                    ficResourceRepository.offlineResourceById(resource.getId());
                    log.info("[StoryboardVideoAlgoTaskProcessor.singleTaskSuccessPostProcess] 下线旧资源, resourceId: {}", resource.getId());
                    break;
                }
            }
            FicResourceBO ficResourceBO = new FicResourceBO();
            ficResourceBO.setWorkflowId(workflowId);
            ficResourceBO.setResourceType(ResourceTypeEnum.STORYBOARD_VIDEO.name());
            ficResourceBO.setResourceUrl(uploadResp.getData());
            ficResourceBO.setRelevanceId(algoTask.getRelevantId());
            ficResourceBO.setRelevanceType(algoTask.getRelevantIdType());
            ficResourceBO.setResourceStorageType(ResourceStorageTypeEnum.ALI_CLOUD_OSS.name());
            ficResourceBO.setStatus(CommonStatusEnum.VALID.getValue());
            ficResourceBO.setGmtCreate(System.currentTimeMillis());
            ficResourceBO.setOriginName(storyboardVideoResult.getOriginalFilename());
            ficResourceRepository.insert(ficResourceBO);
            log.info("[StoryboardVideoAlgoTaskProcessor.singleTaskSuccessPostProcess] 分镜视频处理完成, algoTaskId: {}, resourceId: {}", algoTask.getAlgoTaskId(), ficResourceBO.getId());
        } catch (Exception e) {
            log.error("[StoryboardVideoAlgoTaskProcessor.singleTaskSuccessPostProcess] 处理分镜视频失败, algoTaskId: {}", algoTask.getAlgoTaskId(), e);
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
