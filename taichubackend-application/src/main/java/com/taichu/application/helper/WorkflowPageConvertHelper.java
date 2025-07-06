package com.taichu.application.helper;

import com.taichu.domain.enums.WorkflowStatusEnum;
import com.taichu.domain.enums.AlgoTaskTypeEnum;
import com.taichu.domain.enums.TaskStatusEnum;
import com.taichu.domain.model.FicWorkflowTaskBO;
import com.taichu.domain.model.FicAlgoTaskBO;
import com.taichu.infra.repo.FicAlgoTaskRepository;
import com.taichu.sdk.constant.WorkflowPageEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工作流页面转换助手
 * 负责根据工作流状态和运行中的任务状态，确定当前应该显示的工作流页面
 */
@Slf4j
@Component
public class WorkflowPageConvertHelper {

    @Autowired
    private FicAlgoTaskRepository ficAlgoTaskRepository;

    /**
     * 根据工作流状态和运行中的任务，转换为对应的页面枚举
     * 
     * @param workflowStatusEnum 工作流状态枚举
     * @param runningTask 当前运行中的工作流任务，可能为null
     * @return 对应的页面枚举，如果无法确定则返回null
     */
    public WorkflowPageEnum convertToWorkflowPage(WorkflowStatusEnum workflowStatusEnum, FicWorkflowTaskBO runningTask) {
        if (workflowStatusEnum == null) {
            return null;
        }

        // 页面1：文件上传页面
        if (isUploadFilePage(workflowStatusEnum)) {
            return WorkflowPageEnum.PAGE_1_UPLOAD_FILE;
        }

        // 页面2：剧本页面
        if (isScriptPage(workflowStatusEnum, runningTask)) {
            return WorkflowPageEnum.PAGE_2_SCRIPT;
        }

        // 页面3：分镜图片页面
        if (isStoryboardImgPage(workflowStatusEnum, runningTask)) {
            return WorkflowPageEnum.PAGE_3_STORYBOARD_IMG;
        }

        // 页面4：分镜视频页面
        if (isStoryboardVideoPage(workflowStatusEnum, runningTask)) {
            return WorkflowPageEnum.PAGE_4_STORYBOARD_VIDEO;
        }

        // 页面5：完整视频合成页面
        if (WorkflowStatusEnum.FULL_VIDEO_GEN_DONE.equals(workflowStatusEnum)) {
            return WorkflowPageEnum.PAGE_5_MERGE_VIDEO;
        }

        return null;
    }

    /**
     * 判断是否为文件上传页面
     * 包括：初始化等待文件、文件上传完成、剧本生成初始化
     */
    private boolean isUploadFilePage(WorkflowStatusEnum workflowStatusEnum) {
        return WorkflowStatusEnum.INIT_WAIT_FOR_FILE.equals(workflowStatusEnum)
                || WorkflowStatusEnum.UPLOAD_FILE_DONE.equals(workflowStatusEnum)
                || WorkflowStatusEnum.SCRIPT_GEN_INIT.equals(workflowStatusEnum);
    }

    /**
     * 判断是否为剧本页面
     * 包括：剧本生成完成、分镜图片生成初始化（且没有图片已完成）
     */
    private boolean isScriptPage(WorkflowStatusEnum workflowStatusEnum, FicWorkflowTaskBO runningTask) {
        // 剧本生成完成
        if (WorkflowStatusEnum.SCRIPT_GEN_DONE.equals(workflowStatusEnum)) return true;

        // 分镜图片生成初始化（且没有图片已完成）
        if (WorkflowStatusEnum.STORYBOARD_IMG_GEN_INIT.equals(workflowStatusEnum)) {
            return !hasAnyCompletedTasks(runningTask, AlgoTaskTypeEnum.STORYBOARD_IMG_GENERATION);
        }

        // 分镜图片生成初始化状态，且有图片已完成
        if (WorkflowStatusEnum.STORYBOARD_IMG_GEN_INIT.equals(workflowStatusEnum)) {
            return hasAnyCompletedTasks(runningTask, AlgoTaskTypeEnum.STORYBOARD_IMG_GENERATION);
        }

        return false;
    }

    /**
     * 判断是否为分镜图片页面
     * 包括：分镜图片生成完成、分镜视频生成初始化（且没有视频已完成）
     * 或者分镜图片生成初始化且已有图片生成完成
     */
    private boolean isStoryboardImgPage(WorkflowStatusEnum workflowStatusEnum, FicWorkflowTaskBO runningTask) {
        // 分镜图片生成完成状态
        if (WorkflowStatusEnum.STORYBOARD_IMG_GEN_DONE.equals(workflowStatusEnum)) {
            return true;
        }

        // 分镜视频生成初始化状态，但没有视频已完成
        if (WorkflowStatusEnum.STORYBOARD_VIDEO_GEN_INIT.equals(workflowStatusEnum)) {
            return !hasAnyCompletedTasks(runningTask, AlgoTaskTypeEnum.STORYBOARD_VIDEO_GENERATION);
        }

        // 分镜视频生成初始化状态，且有视频已完成
        if (WorkflowStatusEnum.STORYBOARD_VIDEO_GEN_INIT.equals(workflowStatusEnum)) {
            return hasAnyCompletedTasks(runningTask, AlgoTaskTypeEnum.STORYBOARD_VIDEO_GENERATION);
        }

        return false;
    }

    /**
     * 判断是否为分镜视频页面
     * 包括：分镜视频生成完成、完整视频生成初始化
     * 或者分镜视频生成初始化且已有视频生成完成
     */
    private boolean isStoryboardVideoPage(WorkflowStatusEnum workflowStatusEnum, FicWorkflowTaskBO runningTask) {
        // 分镜视频生成完成状态
        if (WorkflowStatusEnum.STORYBOARD_VIDEO_GEN_DONE.equals(workflowStatusEnum)) {
            return true;
        }

        // 完整视频生成初始化状态
        if (WorkflowStatusEnum.FULL_VIDEO_GEN_INIT.equals(workflowStatusEnum)) {
            return true;
        }

        return false;
    }

    /**
     * 检查指定工作流任务中是否有指定类型的算法任务已完成
     * 
     * @param runningTask 运行中的工作流任务，可能为null
     * @param algoTaskType 算法任务类型
     * @return 如果有任何任务已完成则返回true，否则返回false
     */
    private boolean hasAnyCompletedTasks(FicWorkflowTaskBO runningTask, AlgoTaskTypeEnum algoTaskType) {
        // 如果运行中的任务为空，则没有已完成的任务
        if (runningTask == null) {
            return false;
        }

        try {
            // 查询指定工作流任务下的指定类型算法任务
            List<FicAlgoTaskBO> algoTasks = ficAlgoTaskRepository.findByWorkflowTaskIdAndTaskType(
                    runningTask.getId(), algoTaskType);
            
            // 如果没有找到算法任务，则没有已完成的任务
            if (algoTasks == null || algoTasks.isEmpty()) {
                return false;
            }
            
            // 检查是否有任何任务状态为已完成
            return algoTasks.stream()
                    .anyMatch(task -> TaskStatusEnum.COMPLETED.getCode().equals(task.getStatus()));
        } catch (Exception e) {
            // 如果查询失败，保守起见返回false，避免影响主流程
            log.error(e.getMessage(), e);
            return false;
        }
    }
}
