package com.taichu.domain.model;

import com.taichu.domain.enums.RelevanceType;
import lombok.Data;

/**
 * 任务业务对象
 */
@Data
public class FicAlgoTaskBO {
    
    /**
     * 任务ID
     */
    private Long id;

    /**
     * 创建时间
     */
    private Long gmtCreate;

    /**
     * 工作流任务ID
     */
    private Long workflowTaskId;

    /**
     * @see com.taichu.domain.enums.CommonStatusEnum
     */
    private Byte status;

    /**
     * 任务类型
     */
    private String taskType;    

    /**
     * 算法任务ID
     */
    private String algoTaskId;

    /**
     * @see RelevanceType
     */
    private String relevantIdType;

    /**
     *
     */
    private Long relevantId;

    /**
     * 错误信息
     */
    private String taskAbstract;

    public String buildSummary() {
        return "FicAlgoTaskBO{" +
                "id=" + id +
                ", gmtCreate=" + gmtCreate +
                ", workflowTaskId=" + workflowTaskId +
                ", taskType='" + taskType + '\'' +
                ", algoTaskId='" + algoTaskId + '\'' +
                ", relevantIdType='" + relevantIdType + '\'' +
                ", relevantId=" + relevantId +
                '}';
    }
}