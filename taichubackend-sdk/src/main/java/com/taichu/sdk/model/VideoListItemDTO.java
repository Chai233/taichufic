package com.taichu.sdk.model;

import lombok.Data;

/**
 * 视频对象
 */
@Data
public class VideoListItemDTO {
    /**
     * 展示顺序，由小到大排序
     */
    private Long orderIndex;
    /**
     * 视频首帧图片url
     */
    private String thumbnailUrl;
    /**
     * 分镜id
     */
    private Long storyboardId;
    /**
     * 分镜视频资源id
     */
    private Long storyboardResourceId;
}
