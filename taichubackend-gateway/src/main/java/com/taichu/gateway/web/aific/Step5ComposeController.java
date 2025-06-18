package com.taichu.gateway.web.aific;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.service.ComposeVideoAppService;
import com.taichu.application.service.user.util.AuthUtil;
import com.taichu.sdk.model.FullVideoListItemDTO;
import com.taichu.sdk.model.WorkflowTaskStatusDTO;
import com.taichu.sdk.model.request.ComposeVideoRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.taichu.common.common.exception.ControllerExceptionHandle;

import java.util.Optional;

/**
 * 视频合成相关接口控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/compose")
public class Step5ComposeController {

    private final ComposeVideoAppService composeVideoAppService;

    public Step5ComposeController(ComposeVideoAppService composeVideoAppService) {
        this.composeVideoAppService = composeVideoAppService;
    }

    /**
     * 提交视频合成任务
     *
     * @param request 合成请求参数
     * @return 任务ID
     */
    @PostMapping("/generate")
    @ControllerExceptionHandle(biz = "Step5Compose")
    public SingleResponse<Long> generateComposeVideo(@RequestBody ComposeVideoRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        return composeVideoAppService.submitComposeVideoTask(request, userId);
    }

    /**
     * 重新提交视频合成任务
     *
     * @param request 合成请求参数
     * @return 任务ID
     */
    @PostMapping("/userReGenerate")
    @ControllerExceptionHandle(biz = "Step5Compose")
    public SingleResponse<Long> reGenerateComposeVideo(@RequestBody ComposeVideoRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        return composeVideoAppService.submitReComposeVideoTask(request, userId);
    }

    /**
     * 获取任务进度
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    @GetMapping("/task/status")
    public SingleResponse<WorkflowTaskStatusDTO> getComposeTaskStatus(@RequestParam("taskId") Long taskId) {
        return composeVideoAppService.getComposeTaskStatus(taskId);
    }

    /**
     * 下载合成视频
     *
     * @param workflowId 工作流ID
     * @return 视频文件
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadComposeVideo(@RequestParam Long workflowId) {
        if (workflowId == null) {
            log.error("workflowId is null");
            return ResponseEntity.badRequest().build();
        }

        Optional<Resource> resourceOpt = composeVideoAppService.downloadComposeVideo(workflowId);
        if (resourceOpt.isEmpty()) {
            log.error("No video found for workflow: {}", workflowId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Resource resource = resourceOpt.get();
        if (resource == null) {
            log.error("Video resource is null for workflow: {}", workflowId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"compose_video.mp4\"")
                .body(resource);
    }

    /**
     * 获取视频信息
     *
     * @param workflowId 工作流ID
     * @return 视频信息列表
     */
    @GetMapping("/getAll")
    public MultiResponse<FullVideoListItemDTO> getComposeVideo(@RequestParam Long workflowId) {
        return composeVideoAppService.getComposeVideo(workflowId);
    }

    // @GetMapping("/getResource")
    // @ApiOperation(value = "获取视频", notes = "")
    // public ResponseEntity<Resource> getVideo(@RequestParam Long resourceId) {
    //     return composeVideoAppService.getVideoResource(resourceId)
    //             .map(resource -> ResponseEntity.ok()
    //                     .contentType(MediaType.APPLICATION_OCTET_STREAM)
    //                     .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"video.mp4\"")
    //                     .body(resource))
    //             .orElse(ResponseEntity.notFound().build());
    // }
}

