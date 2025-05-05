package com.taichu.gateway.web.aific;

import com.alibaba.cola.dto.SingleResponse;
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
@Api(tags = "Page1 - 文件接口")
public class FileController {

    @PostMapping("/upload")
    @ApiOperation(value = "上传文件", notes = "上传多个文件")
    public SingleResponse<?> uploadFiles(@ApiParam(required = true) @RequestParam("files") List<MultipartFile> files,
                                         @ApiParam(required = true) @RequestParam Long workflowId) {
        // 批量上传文件
        return SingleResponse.buildSuccess();
    }
}
