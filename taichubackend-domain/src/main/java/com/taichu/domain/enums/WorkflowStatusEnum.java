package com.taichu.domain.enums;

/**
 * 工作流状态枚举
 */
public enum WorkflowStatusEnum {
    /**
     * 初始化
     */
    INIT_WAIT_FOR_FILE((byte) 0, "初始化"),
    
    /**
     * 文件上传完成
     */
    UPLOAD_FILE_DONE((byte) 1, "文件上传完成"),
    
    /**
     * 剧本生成
     */
    SCRIPT_GEN_INIT((byte) 2, "剧本生成初始化"),

    SCRIPT_GEN_DONE((byte) 3, "剧本生成完成"),
    
    /**
     * 分镜图片生成
     */
    STORYBOARD_IMG_GEN_INIT((byte) 4, "分镜图片生成"),

    STORYBOARD_IMG_GEN_DONE((byte) 5, "分镜图片完成"),
    
    /**
     * 分镜视频生成
     */
    STORYBOARD_VIDEO_GEN_INIT((byte) 6, "分镜视频生成"),

    STORYBOARD_VIDEO_GEN_DONE((byte) 7, "分镜视频生成完成"),
    
    /**
     * 完整视频生成
     */
    FULL_VIDEO_GEN_INIT((byte) 8, "完整视频生成"),

    FULL_VIDEO_GEN_DONE((byte) 9, "完整视频生成完成"),
    
    /**
     * 关闭
     */
    CLOSE((byte) 10, "关闭");

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