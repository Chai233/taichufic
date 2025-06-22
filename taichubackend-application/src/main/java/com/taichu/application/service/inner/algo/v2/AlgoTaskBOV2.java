package com.taichu.application.service.inner.algo.v2;

import com.taichu.domain.enums.RelevanceType;
import lombok.Getter;
import lombok.Setter;

/**
 * 算法任务业务对象
 */
@Setter
@Getter
public class AlgoTaskBOV2 {
    private String algoTaskId;
    private Long relevantId;
    private RelevanceType relevantIdType;
    private String taskSummary;

}