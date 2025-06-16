package com.taichu.sdk.model;

import lombok.Data;

import java.util.List;

@Data
public class RoleVO {
    /**
     * 角色id
     */
    Long roleId;
    /**
     * 角色名称
     */
    String roleName;
    /**
     * 描述说明
     */
    String description;
    /**
     * 选中头像图片
     */
    RoleImageVO selectedImage;
    /**
     * 所有头像图片
     */
    List<RoleImageVO> allImageList;

    @Data
    public static class RoleImageVO {
        /**
         * 资源id
         */
        Long resourceId;
        /**
         * 资源下载url（OSS url）
         */
        String resourceUrl;
    }
}
