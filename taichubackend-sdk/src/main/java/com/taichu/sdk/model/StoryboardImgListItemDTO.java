package com.taichu.sdk.model;

import lombok.Data;

/**
 * 分镜图对象
 */
@Data
public class StoryboardImgListItemDTO {
    /**
     * 展示顺序，由小到大排序
     */
    private Long orderIndex;
    /**
     * 分镜缩略图url
     */
    private String thumbnailUrl;
    /**
     * 分镜图url
     */
    private String imgUrl;
    /**
     * 分镜id
     */
    private Long storyboardId;
    /**
     * 分镜资源id
     */
    private Long storyboardResourceId;
}
