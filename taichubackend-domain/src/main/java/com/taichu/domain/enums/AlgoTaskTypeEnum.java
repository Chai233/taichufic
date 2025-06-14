package com.taichu.domain.enums;

/**
 * 任务类型枚举
 */
public enum AlgoTaskTypeEnum {
    /**
     * 剧本生成
     */
    SCRIPT_GENERATION,

    /**
     * 角色图片生成
     */
    ROLE_IMG_GENERATION,

    /**
     * 分镜生成
     */
    STORYBOARD_TEXT_GENERATION,

    /**
     * 分镜图片生成
     */
    STORYBOARD_IMG_GENERATION,

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