package com.taichu.domain.model;

import com.taichu.domain.enums.TaskStatusEnum;
import lombok.Data;

/**
 * 任务状态
 */
@Data
public class AlgoTaskStatus {
    /**
     * 状态码
     */
    private Byte code;

    /**
     * 是否完成
     */
    public boolean isCompleted() {
        return TaskStatusEnum.COMPLETED.getCode().equals(code);
    }

    /**
     * 是否失败
     */
    public boolean isFailed() {
        return TaskStatusEnum.FAILED.getCode().equals(code);
    }

    /**
     * 是否在运行中
     * @return
     */
    public boolean isRunning() {
        return TaskStatusEnum.RUNNING.getCode().equals(code);
    }


} 