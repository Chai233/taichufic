package com.taichu.domain.model.role;

import lombok.Data;

import java.util.List;

@Data
public class RoleFullBO {
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
    RoleImgBO selectedImage;
    /**
     * 所有头像图片
     */
    List<RoleImgBO> allImageList;
}
