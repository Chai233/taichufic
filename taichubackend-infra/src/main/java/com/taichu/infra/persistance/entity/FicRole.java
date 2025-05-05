package com.taichu.infra.persistance.entity;

import lombok.Data;

@Data
public class FicRole {
    private Long id;
    private Long workflowId;
    private Long gmtCreate;
    private Integer status;
    private String roleName;
    private String description;
    private String prompt;
    private String extendInfo;
} 