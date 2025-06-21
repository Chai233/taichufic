package com.taichu.application.service.inner.algo;

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
public class RoleImgAlgoTaskProcessor extends AbstractAlgoTaskProcessor {
    private final FicStoryboardRepository ficStoryboardRepository;
    private final AlgoGateway algoGateway;
    private final FicRoleRepository ficRoleRepository;
    private final FileGateway fileGateway;
    private final FicResourceRepository ficResourceRepository;

    public RoleImgAlgoTaskProcessor(FicStoryboardRepository ficStoryboardRepository, AlgoGateway algoGateway, FicRoleRepository ficRoleRepository, FileGateway fileGateway, FicResourceRepository ficResourceRepository, FicWorkflowTaskRepository ficWorkflowTaskRepository, FicWorkflowRepository ficWorkflowRepository) {
        super(ficWorkflowTaskRepository, ficWorkflowRepository);
        this.ficStoryboardRepository = ficStoryboardRepository;
        this.algoGateway = algoGateway;
        this.ficRoleRepository = ficRoleRepository;
        this.fileGateway = fileGateway;
        this.ficResourceRepository = ficResourceRepository;
    }

    @Override
    public AlgoTaskTypeEnum getAlgoTaskType() {
        return AlgoTaskTypeEnum.ROLE_IMG_GENERATION;
    }

    @Override
    public List<AlgoTaskBO> generateTasks(FicWorkflowTaskBO workflowTask) {
        Long workflowId = workflowTask.getWorkflowId();
        log.info("[RoleImgAlgoTaskProcessor.generateTasks] 开始生成角色图片任务, workflowId: {}", workflowId);
        // 1. 查询角色
        List<FicRoleBO>  ficRoleBOList = ficRoleRepository.findByWorkflowId(workflowId);
        log.info("[RoleImgAlgoTaskProcessor.generateTasks] 查询到角色: {}", ficRoleBOList);
        if (ficRoleBOList.isEmpty()) {
            log.warn("[RoleImgAlgoTaskProcessor.generateTasks] 角色为空, workflowId: {}", workflowId);
            return List.of();
        }
        List<AlgoTaskBO> resultList = new ArrayList<>(ficRoleBOList.size());
        for (FicRoleBO ficRoleBO : ficRoleBOList) {
            log.info("[RoleImgAlgoTaskProcessor.generateTasks] 处理角色, roleId: {}", ficRoleBO.getId());
            // 调用算法服务
            String operationName = "Call algorithm service for workflow: " + workflowId;
            AlgoResponse response = callAlgoServiceWithRetry(operationName, () -> callAlgoServiceGenStoryboardImg(workflowTask, ficRoleBO));
            log.info("[RoleImgAlgoTaskProcessor.generateTasks] 算法服务响应: {}", response);
            // 检查算法服务响应
            if (response == null) {
                log.error("[RoleImgAlgoTaskProcessor.generateTasks] Algorithm service failed to create script task for workflow: {}, after {} retries", workflowId, getMaxRetry());
                return Collections.emptyList();
            }
            AlgoTaskBO algoTaskBO = new AlgoTaskBO();
            algoTaskBO.setAlgoTaskId(response.getTaskId());
            algoTaskBO.setRelevantId(ficRoleBO.getId());
            algoTaskBO.setRelevantIdType(RelevanceType.ROLE_ID);
            resultList.add(algoTaskBO);
            log.info("[RoleImgAlgoTaskProcessor.generateTasks] 添加任务: {}", algoTaskBO);
        }
        return resultList;
    }

    protected AlgoResponse callAlgoServiceGenStoryboardImg(FicWorkflowTaskBO workflowTask, FicRoleBO ficRoleBO) {
        Long workflowId = workflowTask.getWorkflowId();

        // 构建请求参数并调用算法服务
        RoleImageRequest.RoleInfo roleInfo = new RoleImageRequest.RoleInfo();
        roleInfo.setRole(ficRoleBO.getRoleName());
        roleInfo.setPrompt(ficRoleBO.getPrompt());

        RoleImageRequest request = new RoleImageRequest();
        request.setImage_num(4);
        request.setRole_info(roleInfo);
        request.setImage_style(ImageVideoStyleEnum.CYBER_PUNK.getValue());       // TODO 从 workflowMeta获取
        request.setWorkflow_id(Objects.toString(workflowId));

        return algoGateway.createRoleImageTask(request);
    }

    @Override
    public TaskStatusEnum checkSingleTaskStatus(FicAlgoTaskBO algoTask) {
        AlgoTaskStatus taskStatus = algoGateway.checkTaskStatus(Objects.toString(algoTask.getAlgoTaskId()));
        if (taskStatus.isCompleted()) {
            return TaskStatusEnum.COMPLETED;
        } else if (taskStatus.isRunning()) {
            return TaskStatusEnum.RUNNING;
        }

        return TaskStatusEnum.FAILED;
    }

    @Override
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask) throws Exception {
        log.info("[RoleImgAlgoTaskProcessor.singleTaskSuccessPostProcess] 开始处理角色图片, algoTaskId: {}", algoTask.getAlgoTaskId());
        
        // 获取角色图片压缩包
        MultipartFile roleImageZip = algoGateway.getRoleImageResult(Objects.toString(algoTask.getAlgoTaskId()));
        if (roleImageZip == null) {
            throw new Exception("[RoleImgAlgoTaskProcessor.singleTaskSuccessPostProcess] 获取角色图片结果失败, algoTaskId: " + algoTask.getAlgoTaskId());
        }

        // 解压图片
        List<MultipartFile> unzippedImages = unzipMultipartFile(roleImageZip);
        log.info("[RoleImgAlgoTaskProcessor.singleTaskSuccessPostProcess] 解压图片数量: {}", unzippedImages.size());
        if (unzippedImages.isEmpty()) {
            throw new Exception("[RoleImgAlgoTaskProcessor.singleTaskSuccessPostProcess] 解压角色图片失败, algoTaskId: " + algoTask.getAlgoTaskId());
        }

        // 获取角色信息
        Long roleId = algoTask.getRelevantId();
        FicRoleBO role = ficRoleRepository.findById(roleId);
        if (role == null) {
            throw new Exception("[RoleImgAlgoTaskProcessor.singleTaskSuccessPostProcess] 未找到对应的角色信息, roleId: " + roleId);
        }

        Long defaultImageResourceId = null;
        
        // 逐个处理每张图片：上传OSS -> 保存resource记录
        for (MultipartFile image : unzippedImages) {
            String originalFilename = image.getOriginalFilename();
            log.info("[RoleImgAlgoTaskProcessor.singleTaskSuccessPostProcess] 开始处理图片: {}", originalFilename);
            
            // 上传到OSS
            Resp<String> resp = fileGateway.saveFile(originalFilename, image);
            if (resp == null || !resp.isSuccess()) {
                throw new Exception("[RoleImgAlgoTaskProcessor.singleTaskSuccessPostProcess] 上传图片到OSS失败, 文件名: " + originalFilename + ", 响应: " + resp);
            }
            
            String ossObjName = resp.getData();
            log.info("[RoleImgAlgoTaskProcessor.singleTaskSuccessPostProcess] 上传图片成功: {}, OSS对象名: {}", originalFilename, ossObjName);
            
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
            resource.setOriginName(originalFilename); // 记录原始文件名
            
            long resourceId = ficResourceRepository.insert(resource);
            if (resourceId <= 0) {
                throw new Exception("[RoleImgAlgoTaskProcessor.singleTaskSuccessPostProcess] 保存图片资源记录失败, 文件名: " + originalFilename);
            }
            
            log.info("[RoleImgAlgoTaskProcessor.singleTaskSuccessPostProcess] 保存图片资源成功, resourceId: {}, 文件名: {}", resourceId, originalFilename);
            
            // 设置第一张图片为默认图片
            if (defaultImageResourceId == null) {
                defaultImageResourceId = resourceId;
            }
        }
        
        // 更新角色默认图片
        if (defaultImageResourceId != null) {
            role.setDefaultImageResourceId(defaultImageResourceId);
            ficRoleRepository.update(role);
            log.info("[RoleImgAlgoTaskProcessor.singleTaskSuccessPostProcess] 更新角色默认图片成功, roleId: {}, resourceId: {}", roleId, defaultImageResourceId);
        }
        
        log.info("[RoleImgAlgoTaskProcessor.singleTaskSuccessPostProcess] 角色图片处理完成, roleId: {}, algoTaskId: {}", roleId, algoTask.getAlgoTaskId());
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
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected AlgoGateway getAlgoGateway() {
        return algoGateway;
    }

}
