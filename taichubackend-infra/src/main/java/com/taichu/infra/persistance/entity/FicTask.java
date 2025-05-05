package com.taichu.infra.persistance.entity;

import lombok.Data;

@Data
public class FicTask {
    private Long id;
    private Long gmtCreate;
    private Long workflowId;
    private Integer status;
    private String taskType;
    private Long relevanceId;
    private String relevanceType;
    private Long algoTaskId;
    private String taskAbstract;
} 