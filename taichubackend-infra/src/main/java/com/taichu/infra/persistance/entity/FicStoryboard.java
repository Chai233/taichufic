package com.taichu.infra.persistance.entity;

import lombok.Data;

@Data
public class FicStoryboard {
    private Long id;
    private Long workflowId;
    private Long gmtCreate;
    private Integer status;
    private Long scriptId;
    private Long orderIndex;
    private String content;
    private String extendInfo;
} 