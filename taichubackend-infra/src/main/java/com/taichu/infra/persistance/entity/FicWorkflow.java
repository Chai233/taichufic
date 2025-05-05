package com.taichu.infra.persistance.entity;

import lombok.Data;

@Data
public class FicWorkflow {
    private Long id;
    private Long userId;
    private Long gmtCreate;
    private Integer status;
    private String extendInfo;
} 