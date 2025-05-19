package com.taichu.infra.domain.gateway;

public enum FicResourceStorageTypeEnum {
    LOCAL_FILE_SYS("local_file_system"),
    OSS("oss");

    private final String value;

    FicResourceStorageTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
