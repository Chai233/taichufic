package com.taichu.sdk.model;

import lombok.Data;

/**
 * 完整视频列表项数据传输对象
 */
@Data
public class FullVideoListItemDTO {

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 工作流ID
     */
    private Long workflowId;

    /**
     * 分镜资源ID
     */
    private Long storyboardResourceId;

    /**
     * 视频资源url
     */
    private String videoResourceUrl;
}
