package com.taichu.gateway.web.aific;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.service.StoryboardVideoAppService;
import com.taichu.sdk.model.request.GenerateVideoRequest;
import com.taichu.sdk.model.request.SingleStoryboardVideoRegenRequest;
import com.taichu.sdk.model.StoryboardWorkflowTaskStatusDTO;
import com.taichu.sdk.model.VideoListItemDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.FileSystemResource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping("/api/v1/video")
@Api(tags = "Page 4 - 分镜视频接口")
public class Step4VideoController {

    private final StoryboardVideoAppService storyboardVideoAppService;

    @Autowired
    public Step4VideoController(StoryboardVideoAppService storyboardVideoAppService) {
        this.storyboardVideoAppService = storyboardVideoAppService;
    }

    @PostMapping("/generate")
    @ApiOperation(value = "提交分镜视频生成任务。返回taskId", notes = "")
    public SingleResponse<Long> generateVideo(@RequestBody GenerateVideoRequest request) {
        // TODO@chai 获取userId
        return storyboardVideoAppService.submitGenVideoTask(request, null);
    }

    @GetMapping("/task/status")
    @ApiOperation(value = "获取任务进度", notes = "")
    public SingleResponse<StoryboardWorkflowTaskStatusDTO> getVideoTaskStatus(@RequestParam("taskId") Long taskId) {
        return storyboardVideoAppService.getVideoTaskStatus(taskId);
    }

    @GetMapping("/getAll")
    @ApiOperation(value = "获取全部视频信息", notes = "")
    public MultiResponse<VideoListItemDTO> getAllVideo(@RequestParam Long workflowId) {
        return storyboardVideoAppService.getAllVideo(workflowId);
    }

    @GetMapping("/getSingle")
    @ApiOperation(value = "获取单个视频信息", notes = "")
    public SingleResponse<VideoListItemDTO> getSingleVideo(@RequestParam Long storyboardId) {
        return storyboardVideoAppService.getSingleVideo(storyboardId);
    }

//    @GetMapping("/getResource")
//    @ApiOperation(value = "获取视频资源", notes = "")
//    public ResponseEntity<Resource> getVideo(@RequestParam Long resourceId) {
//        return ResponseEntity.ok().build();
//    }

    @PostMapping("/regenerate")
    @ApiOperation(value = "分镜视频重新生成修改", notes = "修改单个分镜视频。返回修改任务id")
    public SingleResponse<Long> regenerateSingleVideo(@RequestBody SingleStoryboardVideoRegenRequest request) {
        // TODO@chai 获取userId
        return storyboardVideoAppService.regenerateSingleVideo(request, null);
    }

    @GetMapping("/download")
    @ApiOperation(value = "下载分镜视频", notes = "下载所有分镜视频压缩包")
    public ResponseEntity<Resource> downloadVideoZip(@RequestParam Long workflowId) {
        // 1. 获取所有视频信息
        MultiResponse<VideoListItemDTO> videoListResponse = storyboardVideoAppService.getAllVideo(workflowId);
        if (!videoListResponse.isSuccess() || videoListResponse.getData() == null || videoListResponse.getData().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 2. 创建临时目录存放视频文件
        String tempDir = System.getProperty("java.io.tmpdir");
        String zipFileName = "storyboard_videos_" + workflowId + ".zip";
        String zipFilePath = tempDir + File.separator + zipFileName;

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
            // 3. 下载每个视频并添加到zip文件
            for (VideoListItemDTO video : videoListResponse.getData()) {
                if (video.getStoryboardResourceId() == null) {
                    continue;
                }

                // 获取视频资源
                Resource videoResource = storyboardVideoAppService.getVideoResource(video.getStoryboardResourceId());
                if (videoResource == null) {
                    continue;
                }

                // 添加到zip文件
                String videoFileName = "storyboard_" + video.getOrderIndex() + ".mp4";
                ZipEntry zipEntry = new ZipEntry(videoFileName);
                zipOut.putNextEntry(zipEntry);

                // 复制视频数据到zip文件
                try (InputStream videoStream = videoResource.getInputStream()) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = videoStream.read(buffer)) > 0) {
                        zipOut.write(buffer, 0, len);
                    }
                }
                zipOut.closeEntry();
            }
        } catch (IOException e) {
            log.error("Failed to create video zip file for workflow: " + workflowId, e);
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

