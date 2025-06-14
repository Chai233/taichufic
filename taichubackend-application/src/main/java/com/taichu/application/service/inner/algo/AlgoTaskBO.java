package com.taichu.application.service.inner.algo;

import com.taichu.domain.enums.RelevanceType;

import lombok.Data;

@Data
public class AlgoTaskBO {
    String algoTaskId;
    RelevanceType relevantIdType;
    Long relevantId;
}
