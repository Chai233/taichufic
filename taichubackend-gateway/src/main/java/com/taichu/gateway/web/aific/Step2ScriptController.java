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

/**
 * 剧本相关接口控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/script")
public class Step2ScriptController {

    @Autowired
    private ScriptAndRoleAppService scriptAppService;
    private RoleAppService roleAppService;

    /**
     * 提交剧本生成任务
     *
     * @param request 生成请求参数
     * @return 任务ID
     */
    @PostMapping("/generate")
    public SingleResponse<Long> generateScript(@RequestBody GenerateScriptRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        return scriptAppService.submitGenScriptTask(request, userId);
    }

    /**
     * 重新提交剧本生成任务
     *
     * @param request 生成请求参数
     * @return 任务ID
     */
    @PostMapping("/userReGenerate")
    public SingleResponse<Long> reGenerateScript(@RequestBody GenerateScriptRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        return scriptAppService.submitReGenScriptTask(request, userId);
    }

    /**
     * 查询任务状态
     *
     * @param workflowId 工作流ID
     * @return 任务状态
     */
    @GetMapping("/task/status")
    public SingleResponse<WorkflowTaskStatusDTO> getScriptTaskStatus(@RequestParam("workflow_id") Long workflowId) {
        return scriptAppService.getScriptTaskStatus(workflowId);
    }

    /**
     * 获取剧本内容
     *
     * @param workflowId 工作流ID
     * @return 剧本列表
     */
    @GetMapping("/getScript")
    public MultiResponse<ScriptVO> getScript(@RequestParam Long workflowId) {
        return scriptAppService.getScript(workflowId);
    }

    /**
     * 获取角色列表
     *
     * @param workflowId 工作流ID
     * @return 角色列表
     */
    @GetMapping("/getRoles")
    public MultiResponse<RoleVO> getRoles(@RequestParam Long workflowId) {
        return roleAppService.getRoles(workflowId);
    }

    /**
     * 更新角色默认头像
     *
     * @param request 更新请求
     * @return 更新后的角色列表
     */
    @PostMapping("/updateSelectedImage")
    public MultiResponse<RoleVO> updateSelectedRoleImage(@RequestParam UpdateRoleImageRequest request) {
        return roleAppService.updateSelectedRoleImage(request);
    }

    /**
     * 下载剧本文件
     *
     * @param workflowId 工作流ID
     * @return 剧本压缩包
     */
    @GetMapping("/downloadScript")
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

