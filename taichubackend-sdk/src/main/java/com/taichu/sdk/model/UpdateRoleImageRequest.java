package com.taichu.sdk.model;

import lombok.Data;

@Data
public class UpdateRoleImageRequest {
    /**
     * 角色id
     */
    Long roleId;
    /**
     * 资源id
     */
    Long resourceId;
}
