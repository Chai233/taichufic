package com.taichu.domain.enums;

/**
 * 任务状态枚举
 */
public enum TaskStatusEnum {
    /**
     * 失败
     */
    FAILED((byte) 0, "失败"),
    
    /**
     * 执行中
     */
    RUNNING((byte) 1, "执行中"),
    
    /**
     * 成功
     */
    COMPLETED((byte) 2, "成功");

    private final Byte code;
    private final String description;

    TaskStatusEnum(Byte code, String description) {
        this.code = code;
        this.description = description;
    }

    public Byte getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TaskStatusEnum fromCode(Byte code) {
        for (TaskStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid task status code: " + code);
    }
} 