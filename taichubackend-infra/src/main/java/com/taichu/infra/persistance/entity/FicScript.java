package com.taichu.infra.persistance.entity;

import lombok.Data;

@Data
public class FicScript {
    private Long id;
    private Long workflowId;
    private Long gmtCreate;
    private Integer status;
    private Long orderIndex;
    private String content;
    private String extendInfo;
} 