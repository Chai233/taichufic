package com.taichu.application.service;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.annotation.EntranceLog;
import com.taichu.application.helper.WorkflowValidationHelper;
import com.taichu.application.service.user.util.AuthUtil;
import com.taichu.common.common.exception.AppServiceExceptionHandle;
import com.taichu.common.common.util.StreamUtil;
import com.taichu.domain.enums.*;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.domain.model.FicResourceBO;
import com.taichu.domain.model.FicStoryboardBO;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.infra.persistance.model.FicWorkflow;
import com.taichu.infra.repo.FicAlgoTaskRepository;
import com.taichu.infra.repo.FicResourceRepository;
import com.taichu.infra.repo.FicRoleRepository;
import com.taichu.infra.repo.FicScriptRepository;
import com.taichu.infra.repo.FicStoryboardRepository;
import com.taichu.infra.repo.FicWorkflowRepository;
import com.taichu.infra.repo.FicWorkflowTaskRepository;
import com.taichu.infra.repo.query.SingleWorkflowQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流回滚应用服务
 */
@Slf4j
@Component
public class WorkflowRollbackAppService {

    @Autowired
    private FicWorkflowRepository workflowRepository;

    @Autowired
    private FicWorkflowTaskRepository ficWorkflowTaskRepository;

    @Autowired
    private FicAlgoTaskRepository ficAlgoTaskRepository;

    @Autowired
    private FicScriptRepository ficScriptRepository;

    @Autowired
    private FicRoleRepository ficRoleRepository;

    @Autowired
    private FicStoryboardRepository ficStoryboardRepository;

    @Autowired
    private FicResourceRepository ficResourceRepository;

    @Autowired
    private WorkflowValidationHelper workflowValidationHelper;

    /**
     * 工作流回滚
     * @param workflowId 工作流ID
     * @param target 回滚目标状态
     * @return 回滚结果
     */
    @EntranceLog(bizCode = "工作流回滚")
    @AppServiceExceptionHandle(biz = "工作流回滚")
    public SingleResponse<Void> rollbackWorkflow(Long userId, Long workflowId, WorkflowRollbackTargetEnum target) {
        log.info("[rollbackWorkflow] 开始工作流回滚, workflowId: {}, userId: {}, target: {}",
            workflowId, userId, target.name());

        try {
            // 1. 权限验证和状态校验
            SingleResponse<FicWorkflow> validationResult = performRollbackValidation(workflowId, userId, target);
            if (!validationResult.isSuccess()) {
                return SingleResponse.buildFailure(validationResult.getErrCode(), validationResult.getErrMessage());
            }

            FicWorkflow workflow = validationResult.getData();
            WorkflowStatusEnum currentStatus = WorkflowStatusEnum.fromCode(workflow.getStatus());
            WorkflowStatusEnum targetStatus = convertToWorkflowStatus(target);

            // 2. 执行回滚操作
            performRollback(workflowId, currentStatus, targetStatus);

            // 3. 更新工作流状态
            workflowRepository.updateStatus(workflowId, targetStatus.getCode());

            log.info("[rollbackWorkflow] 工作流回滚成功, workflowId: {}, 从 {} 回滚到 {}", 
                workflowId, currentStatus.getDescription(), targetStatus.getDescription());

            return SingleResponse.buildSuccess();
        } catch (Exception e) {
            log.error("[rollbackWorkflow] 工作流回滚失败, workflowId: {}, userId: {}", workflowId, userId, e);
            return SingleResponse.buildFailure("ROLLBACK_001", "工作流回滚失败: " + e.getMessage());
        }
    }

    /**
     * 执行完整的回滚前校验
     */
    private SingleResponse<FicWorkflow> performRollbackValidation(Long workflowId, Long userId, WorkflowRollbackTargetEnum target) {
        // 1. 基础权限验证（复用现有逻辑）
        SingleWorkflowQuery query = SingleWorkflowQuery.builder()
                .workflowId(workflowId)
                .userId(userId)
                .build();
        
        Optional<FicWorkflow> workflowOpt = workflowRepository.findSingleWorkflow(query);
        
        // 2. 校验工作流是否存在
        if (workflowOpt.isEmpty()) {
            return SingleResponse.buildFailure("WORKFLOW_002", "工作流不存在");
        }
        
        FicWorkflow workflow = workflowOpt.get();
        
        // 3. 校验用户权限（如果不是工作流创建者，检查是否为管理员）
        if (!workflow.getUserId().equals(userId)) {
            // 检查是否为管理员
            if (!AuthUtil.isAdmin()) {
                return SingleResponse.buildFailure("WORKFLOW_001", "用户不是该工作流的创建人，无权限执行此操作");
            }
            log.info("[performRollbackValidation] 管理员权限验证通过, workflowId: {}, userId: {}", workflowId, userId);
        }
        
        // 4. 校验工作流状态（不能是关闭状态）
        if (WorkflowStatusEnum.CLOSE.getCode().equals(workflow.getStatus())) {
            return SingleResponse.buildFailure("WORKFLOW_003", "工作流已关闭，无法执行回滚操作");
        }
        
        WorkflowStatusEnum currentStatus = WorkflowStatusEnum.fromCode(workflow.getStatus());
        WorkflowStatusEnum targetStatus = convertToWorkflowStatus(target);
        
        // 5. 状态流转校验
        SingleResponse<?> statusResult = validateRollbackTargetByFlow(currentStatus, targetStatus);
        if (!statusResult.isSuccess()) {
            return SingleResponse.buildFailure(statusResult.getErrCode(), statusResult.getErrMessage());
        }
        
        // 6. 运行中任务校验
        SingleResponse<?> taskResult = validateNoRunningTasks(workflowId);
        if (!taskResult.isSuccess()) {
            return SingleResponse.buildFailure(taskResult.getErrCode(), taskResult.getErrMessage());
        }
        
        return SingleResponse.of(workflow);
    }

    /**
     * 基于状态流转规则校验回滚目标状态
     */
    private SingleResponse<?> validateRollbackTargetByFlow(WorkflowStatusEnum currentStatus, WorkflowStatusEnum targetStatus) {
        // 定义允许回滚的状态映射
        Map<WorkflowStatusEnum, List<WorkflowStatusEnum>> allowedRollbackMap = new HashMap<>();
        
        // 从完整视频生成完成可以回滚到所有状态
        allowedRollbackMap.put(WorkflowStatusEnum.FULL_VIDEO_GEN_DONE, 
            Arrays.asList(WorkflowStatusEnum.INIT_WAIT_FOR_FILE,
                         WorkflowStatusEnum.UPLOAD_FILE_DONE,
                         WorkflowStatusEnum.SCRIPT_GEN_DONE,
                         WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE,
                         WorkflowStatusEnum.STORYBOARD_VIDEO_GEN_DONE));
        
        // 从分镜视频生成完成可以回滚到分镜图及之前状态
        allowedRollbackMap.put(WorkflowStatusEnum.STORYBOARD_VIDEO_GEN_DONE,
            Arrays.asList(WorkflowStatusEnum.INIT_WAIT_FOR_FILE,
                         WorkflowStatusEnum.UPLOAD_FILE_DONE,
                         WorkflowStatusEnum.SCRIPT_GEN_DONE,
                         WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE));
        
        // 从分镜图生成完成可以回滚到剧本及之前状态
        allowedRollbackMap.put(WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE,
            Arrays.asList(WorkflowStatusEnum.INIT_WAIT_FOR_FILE,
                         WorkflowStatusEnum.UPLOAD_FILE_DONE,
                         WorkflowStatusEnum.SCRIPT_GEN_DONE));
        
        // 从剧本生成完成可以回滚到文件上传及之前状态
        allowedRollbackMap.put(WorkflowStatusEnum.SCRIPT_GEN_DONE,
            Arrays.asList(WorkflowStatusEnum.INIT_WAIT_FOR_FILE,
                         WorkflowStatusEnum.UPLOAD_FILE_DONE));
        
        // 从文件上传完成只能回滚到初始状态
        allowedRollbackMap.put(WorkflowStatusEnum.UPLOAD_FILE_DONE,
            Arrays.asList(WorkflowStatusEnum.INIT_WAIT_FOR_FILE));
        
        // 初始状态不能回滚
        allowedRollbackMap.put(WorkflowStatusEnum.INIT_WAIT_FOR_FILE, new ArrayList<>());
        
        // 校验
        List<WorkflowStatusEnum> allowedTargets = allowedRollbackMap.get(currentStatus);
        if (allowedTargets == null || !allowedTargets.contains(targetStatus)) {
            return SingleResponse.buildFailure("STATUS_005", 
                String.format("当前状态【%s】不允许回滚到目标状态【%s】", 
                    currentStatus.getDescription(), targetStatus.getDescription()));
        }
        
        return SingleResponse.of(true);
    }

    /**
     * 校验是否有运行中的任务
     */
    private SingleResponse<?> validateNoRunningTasks(Long workflowId) {
        List<FicWorkflowTaskBO> workflowTasks = ficWorkflowTaskRepository.findByWorkflowId(workflowId);
        
        // 检查是否有运行中的工作流任务
        List<FicWorkflowTaskBO> runningWorkflowTasks = workflowTasks.stream()
                .filter(task -> TaskStatusEnum.RUNNING.getCode().equals(task.getStatus()))
                .collect(Collectors.toList());
        
        if (!runningWorkflowTasks.isEmpty()) {
            return SingleResponse.buildFailure("TASK_001", 
                String.format("工作流有运行中的任务，无法执行回滚操作。运行中任务数量：%d", 
                    runningWorkflowTasks.size()));
        }
        
        // 检查是否有运行中的算法任务
        List<FicAlgoTaskBO> runningAlgoTasks = new ArrayList<>();
        for (FicWorkflowTaskBO workflowTask : workflowTasks) {
            List<FicAlgoTaskBO> algoTasks = ficAlgoTaskRepository.findByWorkflowTaskId(workflowTask.getId());
            List<FicAlgoTaskBO> running = StreamUtil.toStream(algoTasks)
                    .filter(task -> TaskStatusEnum.RUNNING.getCode().equals(task.getStatus()))
                    .collect(Collectors.toList());
            runningAlgoTasks.addAll(running);
        }
        
        if (!runningAlgoTasks.isEmpty()) {
            return SingleResponse.buildFailure("TASK_002", 
                String.format("工作流有运行中的算法任务，无法执行回滚操作。运行中算法任务数量：%d", 
                    runningAlgoTasks.size()));
        }
        
        return SingleResponse.of(true);
    }

    /**
     * 执行回滚操作
     */
    private void performRollback(Long workflowId, WorkflowStatusEnum currentStatus, WorkflowStatusEnum targetStatus) {
        log.info("[performRollback] 开始执行回滚操作, workflowId: {}, 从 {} 回滚到 {}", 
            workflowId, currentStatus.getDescription(), targetStatus.getDescription());

        // 1. 清理资源（标记为无效）
        cleanupResources(workflowId, currentStatus, targetStatus);

        // 2. 清理数据记录
        cleanupDataRecords(workflowId, currentStatus, targetStatus);

        // 3. 重置任务状态
        resetTaskStatus(workflowId, currentStatus, targetStatus);

        log.info("[performRollback] 回滚操作执行完成, workflowId: {}", workflowId);
    }

    /**
     * 清理资源（标记为无效）
     */
    private void cleanupResources(Long workflowId, WorkflowStatusEnum currentStatus, WorkflowStatusEnum targetStatus) {
        log.info("[cleanupResources] 开始清理资源, workflowId: {}, 从 {} 回滚到 {}", 
            workflowId, currentStatus.getDescription(), targetStatus.getDescription());

        try {
            // 根据回滚目标状态决定清理哪些资源
            if (targetStatus.getCode() <= WorkflowStatusEnum.INIT_WAIT_FOR_FILE.getCode()) {
                // 回滚到初始状态，清理所有资源
                cleanupAllResources(workflowId);
            } else if (targetStatus.getCode() <= WorkflowStatusEnum.UPLOAD_FILE_DONE.getCode()) {
                // 回滚到文件上传完毕，清理角色图片、剧本、分镜相关资源
                cleanupResourcesAfterFileUpload(workflowId);
            } else if (targetStatus.getCode() <= WorkflowStatusEnum.SCRIPT_GEN_DONE.getCode()) {
                // 回滚到剧本生成完毕，清理分镜相关资源
                cleanupResourcesAfterScript(workflowId);
            } else if (targetStatus.getCode() <= WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE.getCode()) {
                // 回滚到分镜图生成完毕，清理分镜视频资源
                cleanupResourcesAfterStoryboardImg(workflowId);
            } else if (targetStatus.getCode() <= WorkflowStatusEnum.STORYBOARD_VIDEO_GEN_DONE.getCode()) {
                // 回滚到分镜视频生成完毕，清理完整视频资源
                cleanupResourcesAfterStoryboardVideo(workflowId);
            }

            log.info("[cleanupResources] 资源清理完成, workflowId: {}", workflowId);
        } catch (Exception e) {
            log.error("[cleanupResources] 资源清理失败, workflowId: {}", workflowId, e);
            // 不抛出异常，继续执行其他清理操作
        }
    }

    /**
     * 清理所有资源
     */
    private void cleanupAllResources(Long workflowId) {
        List<FicResourceBO> allResources = ficResourceRepository.findValidByWorkflowId(workflowId);
        for (FicResourceBO resource : allResources) {
            ficResourceRepository.offlineResourceById(resource.getId());
        }
        log.info("[cleanupAllResources] 清理所有资源完成, workflowId: {}, 资源数量: {}", workflowId, allResources.size());
    }

    /**
     * 清理文件上传后的资源
     */
    private void cleanupResourcesAfterFileUpload(Long workflowId) {
        // 清理角色图片资源
        List<FicResourceBO> roleImages = ficResourceRepository.findValidByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.ROLE_IMAGE);
        for (FicResourceBO resource : roleImages) {
            ficResourceRepository.offlineResourceById(resource.getId());
        }

        // 清理分镜图片资源
        List<FicResourceBO> storyboardImages = ficResourceRepository.findValidByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.STORYBOARD_IMG);
        for (FicResourceBO resource : storyboardImages) {
            ficResourceRepository.offlineResourceById(resource.getId());
        }

        // 清理分镜视频资源
        List<FicResourceBO> storyboardVideos = ficResourceRepository.findValidByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.STORYBOARD_VIDEO);
        for (FicResourceBO resource : storyboardVideos) {
            ficResourceRepository.offlineResourceById(resource.getId());
        }

        // 清理完整视频资源
        List<FicResourceBO> fullVideos = ficResourceRepository.findValidByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.FULL_VIDEO);
        for (FicResourceBO resource : fullVideos) {
            ficResourceRepository.offlineResourceById(resource.getId());
        }

        log.info("[cleanupResourcesAfterFileUpload] 清理文件上传后资源完成, workflowId: {}", workflowId);
    }

    /**
     * 清理剧本生成后的资源
     */
    private void cleanupResourcesAfterScript(Long workflowId) {
        // 清理分镜图片资源
        List<FicResourceBO> storyboardImages = ficResourceRepository.findValidByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.STORYBOARD_IMG);
        for (FicResourceBO resource : storyboardImages) {
            ficResourceRepository.offlineResourceById(resource.getId());
        }

        // 清理分镜视频资源
        List<FicResourceBO> storyboardVideos = ficResourceRepository.findValidByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.STORYBOARD_VIDEO);
        for (FicResourceBO resource : storyboardVideos) {
            ficResourceRepository.offlineResourceById(resource.getId());
        }

        // 清理完整视频资源
        List<FicResourceBO> fullVideos = ficResourceRepository.findValidByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.FULL_VIDEO);
        for (FicResourceBO resource : fullVideos) {
            ficResourceRepository.offlineResourceById(resource.getId());
        }

        log.info("[cleanupResourcesAfterScript] 清理剧本生成后资源完成, workflowId: {}", workflowId);
    }

    /**
     * 清理分镜图生成后的资源
     */
    private void cleanupResourcesAfterStoryboardImg(Long workflowId) {
        // 清理分镜视频资源
        List<FicResourceBO> storyboardVideos = ficResourceRepository.findValidByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.STORYBOARD_VIDEO);
        for (FicResourceBO resource : storyboardVideos) {
            ficResourceRepository.offlineResourceById(resource.getId());
        }

        // 清理完整视频资源
        List<FicResourceBO> fullVideos = ficResourceRepository.findValidByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.FULL_VIDEO);
        for (FicResourceBO resource : fullVideos) {
            ficResourceRepository.offlineResourceById(resource.getId());
        }

        log.info("[cleanupResourcesAfterStoryboardImg] 清理分镜图生成后资源完成, workflowId: {}", workflowId);
    }

    /**
     * 清理分镜视频生成后的资源
     */
    private void cleanupResourcesAfterStoryboardVideo(Long workflowId) {
        // 清理完整视频资源
        List<FicResourceBO> fullVideos = ficResourceRepository.findValidByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.FULL_VIDEO);
        for (FicResourceBO resource : fullVideos) {
            ficResourceRepository.offlineResourceById(resource.getId());
        }

        log.info("[cleanupResourcesAfterStoryboardVideo] 清理分镜视频生成后资源完成, workflowId: {}", workflowId);
    }

    /**
     * 清理数据记录
     */
    private void cleanupDataRecords(Long workflowId, WorkflowStatusEnum currentStatus, WorkflowStatusEnum targetStatus) {
        log.info("[cleanupDataRecords] 开始清理数据记录, workflowId: {}, 从 {} 回滚到 {}", 
            workflowId, currentStatus.getDescription(), targetStatus.getDescription());

        try {
            // 根据回滚目标状态决定清理哪些数据
            if (targetStatus.getCode() <= WorkflowStatusEnum.INIT_WAIT_FOR_FILE.getCode()) {
                // 回滚到初始状态，清理所有数据
                ficScriptRepository.offlineByWorkflowId(workflowId);
                ficRoleRepository.offlineByWorkflowId(workflowId);
                cleanupAllStoryboards(workflowId);
            } else if (targetStatus.getCode() <= WorkflowStatusEnum.UPLOAD_FILE_DONE.getCode()) {
                // 回滚到文件上传完毕，清理剧本、角色、分镜数据
                ficScriptRepository.offlineByWorkflowId(workflowId);
                ficRoleRepository.offlineByWorkflowId(workflowId);
                cleanupAllStoryboards(workflowId);
            } else if (targetStatus.getCode() <= WorkflowStatusEnum.SCRIPT_GEN_DONE.getCode()) {
                // 回滚到剧本生成完毕，清理分镜数据
                cleanupAllStoryboards(workflowId);
            }

            log.info("[cleanupDataRecords] 数据记录清理完成, workflowId: {}", workflowId);
        } catch (Exception e) {
            log.error("[cleanupDataRecords] 数据记录清理失败, workflowId: {}", workflowId, e);
            // 不抛出异常，继续执行其他清理操作
        }
    }

    /**
     * 清理所有分镜数据
     */
    private void cleanupAllStoryboards(Long workflowId) {
        List<FicStoryboardBO> allStoryboards = ficStoryboardRepository.findValidByWorkflowId(workflowId);
        for (FicStoryboardBO storyboard : allStoryboards) {
            ficStoryboardRepository.offlineById(storyboard.getId());
        }
        log.info("[cleanupAllStoryboards] 清理所有分镜数据完成, workflowId: {}, 分镜数量: {}", workflowId, allStoryboards.size());
    }

    /**
     * 重置任务状态
     */
    private void resetTaskStatus(Long workflowId, WorkflowStatusEnum currentStatus, WorkflowStatusEnum targetStatus) {
        log.info("[resetTaskStatus] 开始重置任务状态, workflowId: {}, 从 {} 回滚到 {}", 
            workflowId, currentStatus.getDescription(), targetStatus.getDescription());

        try {
            List<FicWorkflowTaskBO> workflowTasks = ficWorkflowTaskRepository.findByWorkflowId(workflowId);
            
            for (FicWorkflowTaskBO workflowTask : workflowTasks) {
                // 根据回滚目标状态决定重置哪些任务
                if (shouldResetTask(workflowTask, targetStatus)) {
                    // 将工作流任务标记为失败
                    ficWorkflowTaskRepository.updateTaskStatus(workflowTask.getId(), TaskStatusEnum.FAILED);
                    
                    // 将相关的算法任务标记为失败
                    List<FicAlgoTaskBO> algoTasks = ficAlgoTaskRepository.findByWorkflowTaskId(workflowTask.getId());
                    for (FicAlgoTaskBO algoTask : algoTasks) {
                        ficAlgoTaskRepository.updateStatus(algoTask.getId(), TaskStatusEnum.FAILED);
                    }
                    
                    log.info("[resetTaskStatus] 重置任务状态完成, workflowTaskId: {}, 算法任务数量: {}", 
                        workflowTask.getId(), algoTasks.size());
                }
            }

            log.info("[resetTaskStatus] 任务状态重置完成, workflowId: {}", workflowId);
        } catch (Exception e) {
            log.error("[resetTaskStatus] 任务状态重置失败, workflowId: {}", workflowId, e);
            // 不抛出异常，继续执行其他清理操作
        }
    }

    /**
     * 判断是否需要重置任务
     */
    private boolean shouldResetTask(FicWorkflowTaskBO workflowTask, WorkflowStatusEnum targetStatus) {
        String taskType = workflowTask.getTaskType();
        
        // 根据目标状态判断需要重置的任务类型
        if (targetStatus.getCode() <= WorkflowStatusEnum.INIT_WAIT_FOR_FILE.getCode()) {
            // 回滚到初始状态，重置所有任务
            return true;
        } else if (targetStatus.getCode() <= WorkflowStatusEnum.UPLOAD_FILE_DONE.getCode()) {
            // 回滚到文件上传完毕，重置剧本和角色生成任务
            return TaskTypeEnum.SCRIPT_AND_ROLE_GENERATION.name().equals(taskType) ||
                   TaskTypeEnum.USER_RETRY_SCRIPT_AND_ROLE_GENERATION.name().equals(taskType) ||
                   TaskTypeEnum.STORYBOARD_TEXT_AND_IMG_GENERATION.name().equals(taskType) ||
                   TaskTypeEnum.STORYBOARD_VIDEO_GENERATION.name().equals(taskType) ||
                   TaskTypeEnum.FULL_VIDEO_GENERATION.name().equals(taskType);
        } else if (targetStatus.getCode() <= WorkflowStatusEnum.SCRIPT_GEN_DONE.getCode()) {
            // 回滚到剧本生成完毕，重置分镜相关任务
            return TaskTypeEnum.STORYBOARD_TEXT_AND_IMG_GENERATION.name().equals(taskType) ||
                   TaskTypeEnum.STORYBOARD_VIDEO_GENERATION.name().equals(taskType) ||
                   TaskTypeEnum.FULL_VIDEO_GENERATION.name().equals(taskType);
        } else if (targetStatus.getCode() <= WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE.getCode()) {
            // 回滚到分镜图生成完毕，重置分镜视频和完整视频任务
            return TaskTypeEnum.STORYBOARD_VIDEO_GENERATION.name().equals(taskType) ||
                   TaskTypeEnum.FULL_VIDEO_GENERATION.name().equals(taskType);
        } else if (targetStatus.getCode() <= WorkflowStatusEnum.STORYBOARD_VIDEO_GEN_DONE.getCode()) {
            // 回滚到分镜视频生成完毕，重置完整视频任务
            return TaskTypeEnum.FULL_VIDEO_GENERATION.name().equals(taskType);
        }
        
        return false;
    }

    /**
     * 将回滚目标枚举转换为工作流状态枚举
     */
    private WorkflowStatusEnum convertToWorkflowStatus(WorkflowRollbackTargetEnum target) {
        switch (target) {
            case INIT_WAIT_FOR_FILE:
                return WorkflowStatusEnum.INIT_WAIT_FOR_FILE;
            case UPLOAD_FILE_DONE:
                return WorkflowStatusEnum.UPLOAD_FILE_DONE;
            case STORYBOARD_IMG_GEN_DONE:
                return WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE;
            case STORYBOARD_VIDEO_GEN_DONE:
                return WorkflowStatusEnum.STORYBOARD_VIDEO_GEN_DONE;
            default:
                throw new IllegalArgumentException("不支持的回滚目标状态: " + target);
        }
    }
} 