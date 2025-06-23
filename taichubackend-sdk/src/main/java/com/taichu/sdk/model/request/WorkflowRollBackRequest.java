package com.taichu.sdk.model.request;

import lombok.Data;

@Data
public class WorkflowRollBackRequest {

    Long workflowId;

    String targetStatus;
}
