package com.taichu.application.service.inner.algo.v2;

import com.taichu.application.service.inner.algo.v2.context.AlgoTaskContext;
import com.taichu.application.service.inner.algo.v2.context.RoleImgTaskContext;
import com.taichu.application.service.inner.algo.ImageVideoStyleEnum;
import com.taichu.common.common.model.ByteArrayMultipartFile;
import com.taichu.common.common.model.Resp;
import com.taichu.domain.algo.gateway.AlgoGateway;
import com.taichu.domain.algo.gateway.FileGateway;
import com.taichu.domain.algo.model.AlgoResponse;
import com.taichu.domain.algo.model.request.RoleImageRequest;
import com.taichu.domain.enums.*;
import com.taichu.domain.model.*;
import com.taichu.infra.repo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@Slf4j
public class RoleImgAlgoTaskProcessorV2 extends AbstractAlgoTaskProcessorV2 {
    private final AlgoGateway algoGateway;
    private final FicRoleRepository ficRoleRepository;
    private final FileGateway fileGateway;
    private final FicResourceRepository ficResourceRepository;
    private final FicWorkflowMetaRepository ficWorkflowMetaRepository;

    public RoleImgAlgoTaskProcessorV2(AlgoGateway algoGateway,
                                      FicRoleRepository ficRoleRepository,
                                      FileGateway fileGateway,
                                      FicResourceRepository ficResourceRepository,
                                      FicWorkflowTaskRepository ficWorkflowTaskRepository,
                                      FicWorkflowRepository ficWorkflowRepository, FicWorkflowMetaRepository ficWorkflowMetaRepository) {
        super(ficWorkflowTaskRepository, ficWorkflowRepository);
        this.algoGateway = algoGateway;
        this.ficRoleRepository = ficRoleRepository;
        this.fileGateway = fileGateway;
        this.ficResourceRepository = ficResourceRepository;
        this.ficWorkflowMetaRepository = ficWorkflowMetaRepository;
    }

    @Override
    public AlgoTaskTypeEnum getAlgoTaskType() {
        return AlgoTaskTypeEnum.ROLE_IMG_GENERATION;
    }

    @Override
    public List<AlgoTaskContext> createTaskContextList(FicWorkflowTaskBO workflowTask) {
        Long workflowId = workflowTask.getWorkflowId();
        log.info("[RoleImgAlgoTaskProcessorV2.createTaskContextList] 开始创建角色图片任务上下文, workflowId: {}", workflowId);
        
        // 查询角色
        List<FicRoleBO> ficRoleBOList = ficRoleRepository.findByWorkflowId(workflowId);
        if (ficRoleBOList.isEmpty()) {
            log.warn("[RoleImgAlgoTaskProcessorV2.createTaskContextList] 角色为空, workflowId: {}", workflowId);
            return List.of();
        }

        FicWorkflowMetaBO workflowMetaBO = ficWorkflowMetaRepository.findByWorkflowId(workflowId);
        String imageType = ImageVideoStyleEnum.CYBER_PUNK.getValue();
        if (workflowMetaBO != null) {
            imageType = StringUtils.isNotEmpty(workflowMetaBO.getStyleType()) ? workflowMetaBO.getStyleType() : imageType;
        }

        List<AlgoTaskContext> contexts = new ArrayList<>();
        for (FicRoleBO ficRoleBO : ficRoleBOList) {
            RoleImgTaskContext context = new RoleImgTaskContext();
            context.setWorkflowId(workflowId);
            context.setWorkflowTaskId(workflowTask.getId());
            context.setRole(ficRoleBO);
            context.setImageStyle(imageType);
            contexts.add(context);
        }
        
        return contexts;
    }

    @Override
    public void validateContext(AlgoTaskContext context) {
        RoleImgTaskContext roleContext = (RoleImgTaskContext) context;
        if (roleContext.getRole() == null) {
            throw new IllegalArgumentException("角色信息不能为空");
        }
        if (roleContext.getWorkflowId() == null) {
            throw new IllegalArgumentException("工作流ID不能为空");
        }
    }

    @Override
    public AlgoTaskBOV2 generateAlgoTask(AlgoTaskContext context) {
        RoleImgTaskContext roleContext = (RoleImgTaskContext) context;
        
        // 构建请求参数
        RoleImageRequest.RoleInfo roleInfo = new RoleImageRequest.RoleInfo();
        roleInfo.setRole(roleContext.getRole().getRoleName());
        roleInfo.setPrompt(roleContext.getRole().getPrompt());

        RoleImageRequest request = new RoleImageRequest();
        request.setImage_num(4);
        request.setRole_info(roleInfo);
        request.setImage_style(roleContext.getImageStyle());
        request.setWorkflow_id(Objects.toString(roleContext.getWorkflowId()));

        // 调用算法服务（使用基类的重试机制）
        String operationName = "Create role image task for role: " + roleContext.getRole().getRoleName();
        AlgoResponse response = callAlgoServiceWithRetry(operationName, () -> algoGateway.createRoleImageTask(request));
        
        if (response == null) {
            throw new RuntimeException("创建角色图片任务失败");
        }
        
        // 创建AlgoTaskBO
        AlgoTaskBOV2 algoTaskBO = new AlgoTaskBOV2();
        algoTaskBO.setAlgoTaskId(response.getTaskId());
        algoTaskBO.setRelevantId(roleContext.getRole().getId());
        algoTaskBO.setRelevantIdType(RelevanceType.ROLE_ID);
        algoTaskBO.setTaskSummary(roleContext.getTaskSummary());
        
        return algoTaskBO;
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask, AlgoTaskContext context) throws Exception {
        RoleImgTaskContext roleContext = (RoleImgTaskContext) context;
        log.info("[RoleImgAlgoTaskProcessorV2.singleTaskSuccessPostProcess] 开始处理角色图片, algoTaskId: {}", algoTask.getAlgoTaskId());
        
        // 获取角色图片压缩包（使用基类的重试机制）
        String taskId = Objects.toString(algoTask.getAlgoTaskId());
        MultipartFile roleImageZip = retryGetResultOperation(
            () -> algoGateway.getRoleImageResult(taskId),
            "getRoleImageResult",
            taskId
        );
        
        if (roleImageZip == null) {
            throw new Exception("获取角色图片结果失败");
        }

        // 解压图片
        List<MultipartFile> unzippedImages = unzipMultipartFile(roleImageZip);
        if (unzippedImages.isEmpty()) {
            throw new Exception("解压角色图片失败");
        }

        // 获取角色信息
        Long roleId = algoTask.getRelevantId();
        FicRoleBO role = ficRoleRepository.findById(roleId);
        if (role == null) {
            throw new Exception("未找到对应的角色信息");
        }

        // 清理该角色已存在的图片资源
        cleanupExistingRoleImages(roleId);
        Long defaultImageResourceId = null;
        
        // 逐个处理每张图片
        for (MultipartFile image : unzippedImages) {
            String originalFilename = image.getOriginalFilename();
            
            // 上传到OSS
            Resp<String> resp = fileGateway.saveFile(originalFilename, image);
            if (resp == null || !resp.isSuccess()) {
                throw new Exception("上传图片到OSS失败");
            }
            
            String ossObjName = resp.getData();
            
            // 保存resource记录
            FicResourceBO resource = new FicResourceBO();
            resource.setGmtCreate(System.currentTimeMillis());
            resource.setResourceType(ResourceTypeEnum.ROLE_IMAGE.name());
            resource.setResourceUrl(ossObjName);
            resource.setRelevanceId(roleId);
            resource.setRelevanceType(RelevanceType.ROLE_ID.name());
            resource.setWorkflowId(role.getWorkflowId());
            resource.setStatus(CommonStatusEnum.VALID.getValue());
            resource.setResourceStorageType(ResourceStorageTypeEnum.ALI_CLOUD_OSS.name());
            resource.setOriginName(originalFilename);
            
            long resourceId = ficResourceRepository.insert(resource);
            if (resourceId <= 0) {
                throw new Exception("保存图片资源记录失败");
            }
            
            // 设置第一张图片为默认图片
            if (defaultImageResourceId == null) {
                defaultImageResourceId = resourceId;
            }
        }
        
        // 更新角色默认图片
        if (defaultImageResourceId != null) {
            role.setDefaultImageResourceId(defaultImageResourceId);
            ficRoleRepository.update(role);
            log.info("[RoleImgAlgoTaskProcessorV2.singleTaskSuccessPostProcess] 更新角色默认图片成功, roleId: {}, resourceId: {}", roleId, defaultImageResourceId);
        }
        
        log.info("[RoleImgAlgoTaskProcessorV2.singleTaskSuccessPostProcess] 角色图片处理完成, roleId: {}, algoTaskId: {}", roleId, algoTask.getAlgoTaskId());
    }

    @Override
    public void singleTaskFailedPostProcess(FicAlgoTaskBO algoTask, AlgoTaskContext context, Exception e) {
        log.error("[RoleImgAlgoTaskProcessorV2.singleTaskFailedPostProcess] 角色图片生成任务失败: {}", 
            algoTask.buildSummary(), e);
        
        // 清理该角色已存在的图片资源
        try {
            Long roleId = algoTask.getRelevantId();
            cleanupExistingRoleImages(roleId);
            log.info("[RoleImgAlgoTaskProcessorV2.singleTaskFailedPostProcess] 清理失败任务的角色图片完成, roleId: {}", roleId);
        } catch (Exception cleanupException) {
            log.error("[RoleImgAlgoTaskProcessorV2.singleTaskFailedPostProcess] 清理失败任务的角色图片失败, roleId: {}", 
                algoTask.getRelevantId(), cleanupException);
        }
    }

    @Override
    public void postProcessAllComplete(FicWorkflowTaskBO workflowTask, List<AlgoTaskContext> contexts) {
        log.info("[RoleImgAlgoTaskProcessorV2.postProcessAllComplete] 所有角色图片生成任务完成, workflowId: {}", workflowTask.getWorkflowId());
    }

    @Override
    public void postProcessAnyFailed(FicWorkflowTaskBO workflowTask, List<AlgoTaskContext> contexts) {
        super.postProcessAnyFailed(workflowTask, contexts);
        Long workflowId = workflowTask.getWorkflowId();
        cleanupFailedRoleImgTask(workflowId);
    }

    private void cleanupFailedRoleImgTask(Long workflowId) {
        // 清理已创建的角色图片资源
        try {
            List<FicResourceBO> roleImages = ficResourceRepository.findValidByWorkflowIdAndResourceType(workflowId, ResourceTypeEnum.ROLE_IMAGE);
            for (FicResourceBO resource : roleImages) {
                ficResourceRepository.offlineResourceById(resource.getId());
            }
            log.info("[cleanupFailedRoleImgTask] 清理失败任务资源成功, workflowId: {}", workflowId);
        } catch (Exception e) {
            log.error("[cleanupFailedRoleImgTask] 清理失败任务资源失败, workflowId: {}", workflowId, e);
        }
    }

    /**
     * 解压MultipartFile文件
     * @param zipFile 压缩文件
     * @return 解压后的文件列表
     */
    private List<MultipartFile> unzipMultipartFile(MultipartFile zipFile) {
        List<MultipartFile> result = new ArrayList<>();
        try {
            // 创建临时目录
            Path tempDir = Files.createTempDirectory("role_images_");
            
            // 将MultipartFile保存为临时文件
            Path tempZipFile = tempDir.resolve(zipFile.getOriginalFilename());
            zipFile.transferTo(tempZipFile.toFile());

            // 解压文件
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(tempZipFile.toFile()))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        // 创建临时文件
                        Path tempFile = tempDir.resolve(entry.getName());
                        Files.copy(zis, tempFile, StandardCopyOption.REPLACE_EXISTING);
                        
                        // 转换为MultipartFile
                        byte[] content = Files.readAllBytes(tempFile);
                        String contentType = Files.probeContentType(tempFile);
                        ByteArrayMultipartFile multipartFile = new ByteArrayMultipartFile(
                            content,
                            entry.getName(),
                            contentType
                        );
                        result.add(multipartFile);
                    }
                }
            }

            // 清理临时文件
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
            
        } catch (IOException e) {
            log.error("解压文件失败", e);
        }
        return result;
    }

    @Override
    protected AlgoGateway getAlgoGateway() {
        return algoGateway;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    /**
     * 清理角色已存在的图片资源
     * @param roleId 角色ID
     */
    private void cleanupExistingRoleImages(Long roleId) {
        List<FicResourceBO> existingRoleImages = ficResourceRepository.findValidByRelevance(
            roleId, RelevanceType.ROLE_ID.name(), ResourceTypeEnum.ROLE_IMAGE);

        for (FicResourceBO resource : existingRoleImages) {
            ficResourceRepository.offlineResourceById(resource.getId());
            log.debug("[cleanupExistingRoleImages] 清理角色图片资源, roleId: {}, resourceId: {}", roleId, resource.getId());
        }
    }
} 