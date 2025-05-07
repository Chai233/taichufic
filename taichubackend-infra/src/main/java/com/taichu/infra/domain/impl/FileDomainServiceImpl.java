package com.taichu.infra.domain.impl;

import com.taichu.domain.service.FileDomainService;
import com.taichu.infra.file.LocalFileStorageService;
import com.taichu.infra.persistance.mapper.FicResourceMapper;
import com.taichu.infra.persistance.mapper.FicWorkflowMapper;
import com.taichu.infra.persistance.model.FicResource;
import com.taichu.infra.persistance.model.FicWorkflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class FileDomainServiceImpl implements FileDomainService {

    @Autowired
    private LocalFileStorageService localFileStorageService;

    @Autowired
    private FicResourceMapper ficResourceMapper;

    @Autowired
    private FicWorkflowMapper ficWorkflowMapper;

    @Override
    @Transactional
    public boolean saveFile(Long workflowId, List<MultipartFile> files) {
        List<String> savedFilePaths = new ArrayList<>();
        List<Long> savedResourceIds = new ArrayList<>();

        try {
            // 1. 更新workflow状态
            FicWorkflow workflow = new FicWorkflow();
            workflow.setId(workflowId);
            workflow.setStatus((byte)2);
            workflow.setGmtCreate(System.currentTimeMillis());
            ficWorkflowMapper.updateByPrimaryKeySelective(workflow);

            // 2. 保存文件并记录到数据库
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String originalFilename = file.getOriginalFilename();

                // 生成相对路径：workflowId/序号_文件名
                String relativePath = String.format("%d/%d_%s",
                    workflowId,
                    i + 1,
                    originalFilename);

                // 保存文件到本地
                String savedPath = localFileStorageService.saveFile(file, relativePath);
                savedFilePaths.add(savedPath);

                // 保存到数据库
                FicResource resource = new FicResource();
                resource.setRelevanceId(workflowId);
                resource.setResourceType("workflowId");
                resource.setResourceUrl(savedPath);
                resource.setGmtCreate(System.currentTimeMillis());
//                resource.setFileOrder(i + 1);
                ficResourceMapper.insert(resource);
                savedResourceIds.add(resource.getId());
            }
            return true;
        } catch (Exception e) {
            // 回滚：删除已保存的文件和数据库记录
            for (String savedPath : savedFilePaths) {
                try {
                    localFileStorageService.deleteFile(savedPath);
                } catch (IOException ex) {
                    // TODO: 添加日志记录
                    System.err.println("删除文件失败：" + savedPath);
                }
            }

            // 删除已保存的数据库记录
            for (Long resourceId : savedResourceIds) {
                try {
                    ficResourceMapper.deleteByPrimaryKey(resourceId);
                } catch (Exception ex) {
                    // TODO: 添加日志记录
                    System.err.println("删除数据库记录失败：" + resourceId);
                }
            }

            throw new RuntimeException("保存文件失败", e);
        }
    }

    @Override
    public byte[] read(String relativePath) throws IOException {
        return localFileStorageService.readFile(relativePath);
    }
}
