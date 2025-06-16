package com.taichu.gateway.web.aific;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.service.RoleAppService;
import com.taichu.application.service.ScriptAndRoleAppService;
import com.taichu.application.service.user.util.AuthUtil;
import com.taichu.sdk.model.RoleVO;
import com.taichu.sdk.model.UpdateRoleImageRequest;
import com.taichu.sdk.model.request.GenerateScriptRequest;
import com.taichu.sdk.model.ScriptVO;
import com.taichu.sdk.model.WorkflowTaskStatusDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping("/api/v1/script")
@Api(tags = "Page 2 - 剧本接口")
public class Step2ScriptController {

    @Autowired
    private ScriptAndRoleAppService scriptAppService;
    private RoleAppService roleAppService;

    @PostMapping("/generate")
    @ApiOperation(value = "提交剧本生成任务", notes = "")
    public SingleResponse<Long> generateScript(@RequestBody GenerateScriptRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        return scriptAppService.submitGenScriptTask(request, userId);
    }

    @PostMapping("/userReGenerate")
    @ApiOperation(value = "提交剧本生成任务", notes = "")
    public SingleResponse<Long> reGenerateScript(@RequestBody GenerateScriptRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        return scriptAppService.submitReGenScriptTask(request, userId);
    }

    @GetMapping("/task/status")
    @ApiOperation(value = "查询任务状态", notes = "")
    public SingleResponse<WorkflowTaskStatusDTO> getScriptTaskStatus(@RequestParam("workflow_id") Long workflowId) {
        return scriptAppService.getScriptTaskStatus(workflowId);
    }

    @GetMapping("/getScript")
    @ApiOperation(value = "获取剧本", notes = "")
    public MultiResponse<ScriptVO> getScript(@RequestParam Long workflowId) {
        return scriptAppService.getScript(workflowId);
    }

    @GetMapping("/getRoles")
    @ApiOperation(value = "获取角色", notes = "")
    public MultiResponse<RoleVO> getRoles(@RequestParam Long workflowId) {
        return roleAppService.getRoles(workflowId);
    }

    @PostMapping("/updateSelectedImage")
    @ApiOperation(value = "更改默认头像", notes = "")
    public MultiResponse<RoleVO> updateSelectedRoleImage(@RequestParam UpdateRoleImageRequest request) {
        return roleAppService.updateSelectedRoleImage(request);
    }

    @GetMapping("/downloadScript")
    @ApiOperation(value = "下载剧本", notes = "")
    public ResponseEntity<Resource> downloadScript(@RequestParam Long workflowId) {
        // 1. 获取所有剧本信息
        MultiResponse<ScriptVO> scriptListResponse = scriptAppService.getScript(workflowId);
        if (!scriptListResponse.isSuccess() || scriptListResponse.getData() == null || scriptListResponse.getData().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 2. 创建临时目录存放剧本文件
        String tempDir = System.getProperty("java.io.tmpdir");
        String zipFileName = "scripts_" + workflowId + ".zip";
        String zipFilePath = tempDir + File.separator + zipFileName;

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
            // 3. 下载每个剧本并添加到zip文件
            for (ScriptVO script : scriptListResponse.getData()) {
                if (script.getScriptContent() == null) {
                    continue;
                }

                // 添加到zip文件
                String scriptFileName = "script_" + script.getOrder() + ".txt";
                ZipEntry zipEntry = new ZipEntry(scriptFileName);
                zipOut.putNextEntry(zipEntry);

                // 写入剧本内容到zip文件
                zipOut.write(script.getScriptContent().getBytes());
                zipOut.closeEntry();
            }
        } catch (IOException e) {
            log.error("Failed to create script zip file for workflow: " + workflowId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // 4. 创建下载响应
        try {
            File zipFile = new File(zipFilePath);
            FileSystemResource resource = new FileSystemResource(zipFile);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFileName + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .body(resource);
        } catch (Exception e) {
            log.error("Failed to create download response for workflow: " + workflowId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

