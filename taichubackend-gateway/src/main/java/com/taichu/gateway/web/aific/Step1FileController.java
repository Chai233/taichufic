package com.taichu.gateway.web.aific;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.service.FileAppService;
import com.taichu.application.service.user.util.AuthUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传相关接口控制器
 */
@RestController
@RequestMapping("/api/v1/files")
public class Step1FileController {

    private FileAppService fileAppService;

    public Step1FileController(FileAppService fileAppService) {
        this.fileAppService = fileAppService;
    }

    /**
     * 上传文件
     * 支持上传多个文件
     *
     * @param files 文件列表
     * @param workflowId 工作流ID
     * @return 上传结果
     */
    @PostMapping("/upload")
    public SingleResponse<?> uploadFiles(@RequestParam("files") List<MultipartFile> files,
                                         @RequestParam Long workflowId) {
        // 从认证信息中获取用户ID
        Long userId = AuthUtil.getCurrentUserId();
        return fileAppService.uploadFiles(files, workflowId, userId);
    }
}
