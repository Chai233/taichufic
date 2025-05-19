package com.taichu.domain.enums;

/**
 * 工作流状态枚举
 */
public enum WorkflowStatusEnum {
    /**
     * 初始化
     */
    INIT((byte) 0, "初始化"),
    
    /**
     * 文件上传完成
     */
    UPLOAD_FILE_DONE((byte) 1, "文件上传完成"),
    
    /**
     * 脚本生成
     */
    SCRIPT_GEN((byte) 2, "脚本生成"),
    
    /**
     * 分镜图片生成
     */
    STORYBOARD_IMG_GEN((byte) 3, "分镜图片生成"),
    
    /**
     * 分镜视频生成
     */
    STORYBOARD_VIDEO_GEN((byte) 4, "分镜视频生成"),
    
    /**
     * 完整视频生成
     */
    FULL_VIDEO_GEN((byte) 5, "完整视频生成"),
    
    /**
     * 关闭
     */
    CLOSE((byte) 6, "关闭");

    private final Byte code;
    private final String description;

    WorkflowStatusEnum(Byte code, String description) {
        this.code = code;
        this.description = description;
    }

    public Byte getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static WorkflowStatusEnum fromCode(Byte code) {
        for (WorkflowStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid workflow status code: " + code);
    }
}