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

        // 1. 查询角色
        List<FicRoleBO>  ficRoleBOList = ficRoleRepository.findByWorkflowId(workflowId);
        if (ficRoleBOList.isEmpty()) {
            return List.of();
        }

        List<AlgoTaskBO> resultList = new ArrayList<>(ficRoleBOList.size());
        for (FicRoleBO ficRoleBO : ficRoleBOList) {
            // 调用算法服务
            String operationName = "Call algorithm service for workflow: " + workflowId;
            AlgoResponse response = callAlgoServiceWithRetry(operationName, () -> callAlgoServiceGenStoryboardImg(workflowTask, ficRoleBO));

            // 检查算法服务响应
            if (response == null) {
                log.error("Algorithm service failed to create script task for workflow: {}, after {} retries",
                        workflowId, getMaxRetry());
                return Collections.emptyList();
            }

            // 添加到返回列表
            AlgoTaskBO algoTaskBO = new AlgoTaskBO();
            algoTaskBO.setAlgoTaskId(response.getTaskId());
            algoTaskBO.setRelevantId(ficRoleBO.getId());
            algoTaskBO.setRelevantIdType(RelevanceType.ROLE_ID);
            resultList.add(algoTaskBO);
        }

        return resultList;
    }

    protected AlgoResponse callAlgoServiceGenStoryboardImg(FicWorkflowTaskBO workflowTask, FicRoleBO ficRoleBO) {
        Long workflowId = workflowTask.getWorkflowId();

        // 构建请求参数并调用算法服务
        RoleImageRequest request = new RoleImageRequest();
        // TODO 填充参数
    
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
    public void singleTaskSuccessPostProcess(FicAlgoTaskBO algoTask) {
        /*
         * 下载图片，解压缩，然后把每个图片都上传到OSS，然后更新对应角色的默认图片状态
         */
        MultipartFile roleImageZip = algoGateway.getRoleImageResult(Objects.toString(algoTask.getAlgoTaskId()));
        if (roleImageZip == null) {
            log.error("获取角色图片结果失败, algoTaskId: {}", algoTask.getAlgoTaskId());
            return;
        }

        try {
            // 1. 解压图片
            List<MultipartFile> unzippedImages = unzipMultipartFile(roleImageZip);
            if (unzippedImages.isEmpty()) {
                log.error("解压角色图片失败, algoTaskId: {}", algoTask.getAlgoTaskId());
                return;
            }

            // 2. 上传图片到OSS
            List<String> ossObjNames = new ArrayList<>();
            for (MultipartFile image : unzippedImages) {
                Resp<String> resp = fileGateway.saveFile(image.getOriginalFilename(), image);
                if (resp != null && resp.isSuccess()) {
                    ossObjNames.add(resp.getData());
                }
            }

            if (ossObjNames.size() != unzippedImages.size()) {
                log.error("上传角色图片到OSS失败, algoTaskId: {}", algoTask.getAlgoTaskId());
                return;
            }

            // 3. 获取角色信息
            Long roleId = algoTask.getRelevantId();
            FicRoleBO role = ficRoleRepository.findById(roleId);
            if (role == null) {
                log.error("未找到对应的角色信息, roleId: {}", roleId);
                return;
            }

            // 4. 保存所有图片资源
            Long defaultImageResourceId = null;
            for (String ossObjName : ossObjNames) {
                FicResourceBO resource = new FicResourceBO();
                resource.setResourceType(ResourceTypeEnum.ROLE_IMAGE.name());
                resource.setResourceUrl(ossObjName);
                resource.setRelevanceId(roleId);
                resource.setRelevanceType(RelevanceType.ROLE_ID.name());
                resource.setWorkflowId(role.getWorkflowId());
                resource.setStatus(CommonStatusEnum.VALID.getValue());
                long resourceId = ficResourceRepository.insert(resource);
                
                // 第一个图片作为默认图片
                if (defaultImageResourceId == null) {
                    defaultImageResourceId = resourceId;
                }
            }

            // 5. 更新角色的默认图片
            if (defaultImageResourceId != null) {
                role.setDefaultImageResourceId(defaultImageResourceId);
                ficRoleRepository.update(role);
            }

            log.info("角色图片处理完成, roleId: {}, algoTaskId: {}", roleId, algoTask.getAlgoTaskId());
        } catch (Exception e) {
            log.error("处理角色图片失败, algoTaskId: {}, error: {}", algoTask.getAlgoTaskId(), e.getMessage(), e);
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
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected AlgoGateway getAlgoGateway() {
        return algoGateway;
    }

}
