package com.taichu.domain.enums;

/**
 * 任务类型枚举
 */
public enum TaskTypeEnum {
    /**
     * 剧本 & 角色图生成
     */
    SCRIPT_AND_ROLE_GENERATION,

    USER_RETRY_SCRIPT_AND_ROLE_GENERATION,

    /**
     * 分镜片段 & 分镜图生成
     */
    STORYBOARD_TEXT_AND_IMG_GENERATION,

    /**
     * 分镜视频生成
     */
    STORYBOARD_VIDEO_GENERATION,

    /**
     * 完整视频生成
     */
    FULL_VIDEO_GENERATION,

    /**
     * 用户重试单个分镜图片生成
     */
    USER_RETRY_SINGLE_STORYBOARD_IMG_GENERATION,

    /**
     * 用户重试单个分镜视频生成
     */
    USER_RETRY_SINGLE_STORYBOARD_VIDEO_GENERATION,

    /**
     * 用户重试完整视频生成
     */
    USER_RETRY_FULL_VIDEO_GENERATION
} 