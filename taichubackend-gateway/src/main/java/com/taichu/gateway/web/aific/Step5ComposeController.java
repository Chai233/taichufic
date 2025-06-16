package com.taichu.gateway.web.aific;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.service.ComposeVideoAppService;
import com.taichu.application.service.user.util.AuthUtil;
import com.taichu.sdk.model.FullVideoListItemDTO;
import com.taichu.sdk.model.WorkflowTaskStatusDTO;
import com.taichu.sdk.model.request.ComposeVideoRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/compose")
@Api(tags = "Page 5- 视频合成接口")
public class Step5ComposeController {

    private final ComposeVideoAppService composeVideoAppService;

    public Step5ComposeController(ComposeVideoAppService composeVideoAppService) {
        this.composeVideoAppService = composeVideoAppService;
    }

    @PostMapping("/generate")
    @ApiOperation(value = "提交视频合成任务", notes = "")
    public SingleResponse<Long> generateComposeVideo(@RequestBody ComposeVideoRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        return composeVideoAppService.submitComposeVideoTask(request, userId);
    }

    @PostMapping("/userReGenerate")
    public SingleResponse<Long> reGenerateComposeVideo(@RequestBody ComposeVideoRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        return composeVideoAppService.submitReComposeVideoTask(request, userId);
    }

    @GetMapping("/task/status")
    @ApiOperation(value = "获取任务进度", notes = "")
    public SingleResponse<WorkflowTaskStatusDTO> getComposeTaskStatus(@RequestParam("taskId") Long taskId) {
        return composeVideoAppService.getComposeTaskStatus(taskId);
    }

    @GetMapping("/download")
    @ApiOperation(value = "下载视频", notes = "")
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

    @GetMapping("/getAll")
    @ApiOperation(value = "获取视频信息", notes = "")
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

