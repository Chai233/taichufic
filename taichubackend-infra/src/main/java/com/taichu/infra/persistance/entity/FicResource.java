package com.taichu.infra.persistance.entity;

import lombok.Data;

@Data
public class FicResource {
    private Long id;
    private Long gmtCreate;
    private Integer status;
    private Long relevanceId;
    private String relevanceType;
    private String resourceType;
    private String resourceStorageType;
    private String resourceUrl;
    private String extendInfo;
} 