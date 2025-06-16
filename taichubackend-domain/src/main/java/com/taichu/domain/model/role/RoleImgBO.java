package com.taichu.domain.model.role;

import lombok.Data;

@Data
public class RoleImgBO {
    /**
     * 资源id
     */
    Long resourceId;
    /**
     * 资源下载url（OSS url）
     */
    String resourceUrl;
}
