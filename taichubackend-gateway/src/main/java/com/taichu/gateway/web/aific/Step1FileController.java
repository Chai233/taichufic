package com.taichu.gateway.web.aific;

import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.service.FileAppService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@Api(tags = "Page1 - 上传文件")
public class Step1FileController {

    private FileAppService fileAppService;

    public Step1FileController(FileAppService fileAppService) {
        this.fileAppService = fileAppService;
    }

    @PostMapping("/upload")
    @ApiOperation(value = "上传文件", notes = "上传多个文件")
    public SingleResponse<?> uploadFiles(@ApiParam(required = true) @RequestParam("files") List<MultipartFile> files,
                                         @ApiParam(required = true) @RequestParam Long workflowId) {
        // 批量上传文件
        Long userId = null; // TODO 上下文获取userId
        return fileAppService.uploadFiles(files, workflowId, userId);
    }
}
