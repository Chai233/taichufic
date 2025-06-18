package com.taichu.application.service;

import com.alibaba.cola.dto.MultiResponse;
import com.taichu.application.annotation.EntranceLog;
import com.taichu.common.common.exception.AppServiceExceptionHandle;
import com.taichu.domain.enums.ResourceTypeEnum;
import com.taichu.domain.model.FicResourceBO;
import com.taichu.domain.model.FicRoleBO;
import com.taichu.domain.model.role.RoleFullBO;
import com.taichu.domain.model.role.RoleImgBO;
import com.taichu.infra.repo.FicResourceRepository;
import com.taichu.infra.repo.FicRoleRepository;
import com.taichu.sdk.model.RoleVO;
import com.taichu.sdk.model.UpdateRoleImageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RoleAppService {

    @Autowired
    private FicRoleRepository ficRoleRepository;

    @Autowired
    private FicResourceRepository ficResourceRepository;

    @EntranceLog(bizCode = "查询角色")
    @AppServiceExceptionHandle(biz = "查询角色")
    public MultiResponse<RoleVO> getRoles(Long workflowId) {
        List<RoleFullBO> roleFullBOList = getRolesFullBOList(workflowId);
        List<RoleVO> roleVOS = convertRoleBOList(roleFullBOList);
        return MultiResponse.of(roleVOS);
    }

    private List<RoleVO> convertRoleBOList(List<RoleFullBO> roleFullBOList) {
        if (roleFullBOList == null) {
            return new ArrayList<>();
        }
        return roleFullBOList.stream().map(roleFullBO -> {
            RoleVO roleVO = new RoleVO();
            roleVO.setRoleId(roleFullBO.getRoleId());
            roleVO.setRoleName(roleFullBO.getRoleName());
            roleVO.setDescription(roleFullBO.getDescription());
            
            // 设置选中的图片
            if (roleFullBO.getSelectedImage() != null) {
                RoleVO.RoleImageVO selectedImage = new RoleVO.RoleImageVO();
                selectedImage.setResourceId(roleFullBO.getSelectedImage().getResourceId());
                selectedImage.setResourceUrl(roleFullBO.getSelectedImage().getResourceUrl());
                roleVO.setSelectedImage(selectedImage);
            }
            
            // 设置所有图片列表
            if (roleFullBO.getAllImageList() != null) {
                List<RoleVO.RoleImageVO> allImageList = roleFullBO.getAllImageList().stream()
                        .map(img -> {
                            RoleVO.RoleImageVO imageVO = new RoleVO.RoleImageVO();
                            imageVO.setResourceId(img.getResourceId());
                            imageVO.setResourceUrl(img.getResourceUrl());
                            return imageVO;
                        })
                        .collect(Collectors.toList());
                roleVO.setAllImageList(allImageList);
            }
            
            return roleVO;
        }).collect(Collectors.toList());
    }

    public List<RoleFullBO> getRolesFullBOList(Long workflowId) {
        // 1. 获取角色列表
        List<FicRoleBO> roleBOList = ficRoleRepository.findByWorkflowId(workflowId);
        if (roleBOList == null) {
            return new ArrayList<>();
        }

        // 2. 转换为 RoleFullBO 列表
        return roleBOList.stream().map(roleBO -> {
            RoleFullBO roleFullBO = new RoleFullBO();
            roleFullBO.setRoleId(roleBO.getId());
            roleFullBO.setRoleName(roleBO.getRoleName());
            roleFullBO.setDescription(roleBO.getDescription());

            // 3. 获取角色的所有图片资源
            List<FicResourceBO> resourceList = ficResourceRepository.findByWorkflowIdAndResourceType(
                    workflowId, ResourceTypeEnum.ROLE_IMAGE);

            // 4. 过滤出当前角色的图片
            List<FicResourceBO> roleResources = resourceList.stream()
                    .filter(resource -> roleBO.getId().equals(resource.getRelevanceId()))
                    .collect(Collectors.toList());

            // 5. 转换为 RoleImgBO 列表
            List<RoleImgBO> allImageList = roleResources.stream()
                    .map(resource -> {
                        RoleImgBO imgBO = new RoleImgBO();
                        imgBO.setResourceId(resource.getId());
                        imgBO.setResourceUrl(resource.getResourceUrl());
                        return imgBO;
                    })
                    .collect(Collectors.toList());
            roleFullBO.setAllImageList(allImageList);

            // 6. 设置选中的图片（默认图片）
            if (roleBO.getDefaultImageResourceId() != null) {
                FicResourceBO defaultResource = ficResourceRepository.findById(roleBO.getDefaultImageResourceId());
                if (defaultResource != null) {
                    RoleImgBO selectedImage = new RoleImgBO();
                    selectedImage.setResourceId(defaultResource.getId());
                    selectedImage.setResourceUrl(defaultResource.getResourceUrl());
                    roleFullBO.setSelectedImage(selectedImage);
                }
            }

            return roleFullBO;
        }).collect(Collectors.toList());
    }

    @EntranceLog(bizCode = "更换角色默认图片")
    @AppServiceExceptionHandle(biz = "更换角色默认图片")
    public MultiResponse<RoleVO> updateSelectedRoleImage(UpdateRoleImageRequest request) {
        // 1. 获取角色信息
        FicRoleBO role = ficRoleRepository.findById(request.getRoleId());
        if (role == null) {
            log.error("角色不存在, roleId: {}", request.getRoleId());
            return MultiResponse.buildFailure("ROLE_NOT_FOUND", "角色不存在");
        }

        // 2. 获取资源信息
        FicResourceBO resource = ficResourceRepository.findById(request.getResourceId());
        if (resource == null) {
            log.error("资源不存在, resourceId: {}", request.getResourceId());
            return MultiResponse.buildFailure("RESOURCE_NOT_FOUND", "资源不存在");
        }

        // 3. 更新角色的默认图片
        role.setDefaultImageResourceId(request.getResourceId());
        ficRoleRepository.update(role);

        // 4. 返回更新后的角色信息
        return getRoles(role.getWorkflowId());
    }
}
