package com.taichu.sdk.model;

import lombok.Data;

@Data
public class FullVideoListItemDTO {
    private Long orderIndex;

    private String thumbnailUrl;

    private Long workflowId;

    private Long storyboardResourceId;
}
