package com.taichu.domain.enums;

/**
 * 工作流回滚目标状态枚举
 */
public enum WorkflowRollbackTargetEnum {
    /**
     * 初始状态
     */
    INIT_WAIT_FOR_FILE("INIT_WAIT_FOR_FILE", "初始状态"),
    
    /**
     * 文件上传完毕
     */
    UPLOAD_FILE_DONE("UPLOAD_FILE_DONE", "文件上传完毕"),
    
    /**
     * 分镜图生成完毕
     */
    STORYBOARD_IMG_GEN_DONE("STORYBOARD_IMG_GEN_DONE", "分镜图生成完毕"),
    
    /**
     * 分镜视频生成完毕
     */
    STORYBOARD_VIDEO_GEN_DONE("STORYBOARD_VIDEO_GEN_DONE", "分镜视频生成完毕");

    private final String code;
    private final String description;

    WorkflowRollbackTargetEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static WorkflowRollbackTargetEnum fromCode(String code) {
        for (WorkflowRollbackTargetEnum target : values()) {
            if (target.getCode().equals(code)) {
                return target;
            }
        }
        throw new IllegalArgumentException("Invalid workflow rollback target code: " + code);
    }
} 