package com.taichu.gateway.web.aific;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.sdk.model.GenerateVideoRequest;
import com.taichu.sdk.model.SingleStoryboardVideoRegenRequest;
import com.taichu.sdk.model.StoryboardTaskStatusDTO;
import com.taichu.sdk.model.VideoListItemDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/video")
@Api(tags = "Page 4 - 分镜视频接口")
public class VideoController {

    @PostMapping("/generate")
    @ApiOperation(value = "提交分镜视频生成任务。返回taskId", notes = "")
    public SingleResponse<Long> generateVideo(@RequestBody GenerateVideoRequest request) {
        // 提交视频生成任务
        return SingleResponse.buildSuccess();
    }

    @GetMapping("/task/status")
    public SingleResponse<StoryboardTaskStatusDTO> getVideoTaskStatus(@RequestParam("taskId") Long taskId) {
        // 轮询任务结果
        return SingleResponse.buildSuccess();
    }

    @GetMapping("/getAll")
    @ApiOperation(value = "获取全部视频信息", notes = "")
    public MultiResponse<VideoListItemDTO> getAllVideo(@RequestParam Long workflowId) {
        // 获取视频列表
        return MultiResponse.buildSuccess();
    }

    @GetMapping("/getSingle")
    @ApiOperation(value = "获取单个视频信息", notes = "")
    public MultiResponse<VideoListItemDTO> getSingleVideo(@RequestParam Long storyboardId) {
        // 获取视频列表
        return MultiResponse.buildSuccess();
    }

    @GetMapping("/getResource")
    public ResponseEntity<Resource> getVideo(@RequestParam Long resourceId) {
        // 获取单个视频
        return ResponseEntity.ok().build();
    }

    @PostMapping("/regenerate")
    @ApiOperation(value = "分镜视频重新生成修改", notes = "修改单个分镜视频。返回修改任务id")
    public SingleResponse<?> regenerateSingleVideo(@RequestBody SingleStoryboardVideoRegenRequest request) {
        // 单个视频重新生成
        return SingleResponse.buildSuccess();
    }

    @GetMapping("/download")
    @ApiOperation(value = "下载分镜视频", notes = "下载所有分镜视频压缩包")
    public ResponseEntity<Resource> downloadVideoZip(@RequestBody Long workflowId) {
        // 下载视频压缩包
        return ResponseEntity.ok().build();
    }
}

