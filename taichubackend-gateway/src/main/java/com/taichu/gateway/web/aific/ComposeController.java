package com.taichu.gateway.web.aific;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.sdk.model.FullVideoListItemDTO;
import com.taichu.sdk.model.TaskStatusDTO;
import com.taichu.sdk.model.ComposeVideoRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/compose")
@Api(tags = "Page 5- 视频合成接口")
public class ComposeController {

    @PostMapping("/generate")
    public SingleResponse<Long> generateComposeVideo(@RequestBody ComposeVideoRequest request) {
        // 提交视频合成任务
        return SingleResponse.buildSuccess();
    }

    @GetMapping("/task/status")
    public SingleResponse<TaskStatusDTO> getComposeTaskStatus(@RequestParam("taskId") Long taskId) {
        // 轮询任务结果
        return SingleResponse.buildSuccess();
    }

    @GetMapping("/download")
    @ApiOperation(value = "下载视频", notes = "")
    public ResponseEntity<Resource> downloadComposeVideo(@RequestParam Long workflowId) {
        // 下载合成视频
        return ResponseEntity.ok().build();
    }

    @GetMapping("/getAll")
    @ApiOperation(value = "获取单个视频信息", notes = "")
    public MultiResponse<FullVideoListItemDTO> getSingleVideo(@RequestParam Long workflowId) {
        // 获取视频列表
        return MultiResponse.buildSuccess();
    }

    @GetMapping("/getResource")
    @ApiOperation(value = "获取视频", notes = "")
    public ResponseEntity<Resource> getVideo(@RequestParam Long resourceId) {
        // 获取单个视频
        return ResponseEntity.ok().build();
    }
}

