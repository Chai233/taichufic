package com.taichu.gateway.web.aific;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.service.StoryboardImgAppService;
import com.taichu.sdk.model.request.GenerateStoryboardRequest;
import com.taichu.sdk.model.request.SingleStoryboardRegenRequest;
import com.taichu.sdk.model.StoryboardImgListItemDTO;
import com.taichu.sdk.model.StoryboardWorkflowTaskStatusDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.InputStream;

@Slf4j
@RestController
@RequestMapping("/api/v1/storyboard")
@Api(tags = "Page 3 - 分镜图页面")
public class Step3StoryboardImgController {

    private final StoryboardImgAppService storyboardImgAppService;

    public Step3StoryboardImgController(StoryboardImgAppService storyboardImgAppService) {
        this.storyboardImgAppService = storyboardImgAppService;
    }

    /**
     *
     * @param request 请求参数
     * @return taskId
     */
    @PostMapping("/generate")
    @ApiOperation(value = "提交分镜图生成任务。返回taskId", notes = "")
    public SingleResponse<Long> generateStoryboard(@RequestBody GenerateStoryboardRequest request) {
        // 提交分镜生成任务
        // TODO@chai 获取userId
        return storyboardImgAppService.submitGenStoryboardTask(request, null);
    }

    @GetMapping("/task/status")
    @ApiOperation(value = "获取任务进度", notes = "")
    public SingleResponse<StoryboardWorkflowTaskStatusDTO> getStoryboardTaskStatus(@RequestParam("taskId") Long taskId) {
        // 查询任务结果
        return storyboardImgAppService.getStoryboardTaskStatus(taskId);
    }

    @GetMapping("/getAll")
    @ApiOperation(value = "获取全部分镜信息", notes = "")
    public MultiResponse<StoryboardImgListItemDTO> getAllStoryboardImg(@RequestParam Long workflowId) {
        return storyboardImgAppService.getAllStoryboardImg(workflowId);
    }

    @GetMapping("/getSingle")
    @ApiOperation(value = "获取单个分镜信息", notes = "")
    public SingleResponse<StoryboardImgListItemDTO> getSingleStoryboardImg(@RequestParam Long workflowId, @RequestParam Long storyboardId) {
        // 获取分镜列表
        return storyboardImgAppService.getSingleStoryboardImg(workflowId, storyboardId);
    }

    @PostMapping("/regenerate")
    @ApiOperation(value = "分镜图重新生成", notes = "修改单张分镜。返回修改任务id")
    public SingleResponse<Long> regenerateSingleStoryboard(@RequestBody SingleStoryboardRegenRequest request) {
        // 单张分镜重新生成
        Long userId = 1L;
        return storyboardImgAppService.regenerateSingleStoryboard(userId, request);
    }

    @GetMapping("/download")
    @ApiOperation(value = "下载分镜", notes = "下载所有分镜压缩包")
    public ResponseEntity<Resource> downloadStoryboardZip(@RequestParam Long workflowId) {
        // 1. 获取所有分镜信息
        MultiResponse<StoryboardImgListItemDTO> storyboardListResponse = storyboardImgAppService.getAllStoryboardImg(workflowId);
        if (!storyboardListResponse.isSuccess() || storyboardListResponse.getData() == null || storyboardListResponse.getData().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 2. 创建临时目录存放图片文件
        String tempDir = System.getProperty("java.io.tmpdir");
        String zipFileName = "storyboard_images_" + workflowId + ".zip";
        String zipFilePath = tempDir + File.separator + zipFileName;

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
            // 3. 下载每个分镜图片并添加到zip文件
            for (StoryboardImgListItemDTO storyboard : storyboardListResponse.getData()) {
                if (storyboard.getStoryboardResourceId() == null) {
                    continue;
                }

                // 获取图片资源
                Resource imgResource = storyboardImgAppService.getStoryboardResource(storyboard.getStoryboardResourceId());
                if (imgResource == null) {
                    continue;
                }

                // 从URL中获取文件扩展名
                String fileExtension = "png"; // 默认扩展名
                String resourceUrl = storyboard.getImgUrl();
                if (resourceUrl != null) {
                    int lastDotIndex = resourceUrl.lastIndexOf('.');
                    if (lastDotIndex > 0) {
                        fileExtension = resourceUrl.substring(lastDotIndex + 1).toLowerCase();
                    }
                }

                // 添加到zip文件
                String imgFileName = "storyboard_" + storyboard.getOrderIndex() + "." + fileExtension;
                ZipEntry zipEntry = new ZipEntry(imgFileName);
                zipOut.putNextEntry(zipEntry);

                // 复制图片数据到zip文件
                try (InputStream imgStream = imgResource.getInputStream()) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = imgStream.read(buffer)) > 0) {
                        zipOut.write(buffer, 0, len);
                    }
                }
                zipOut.closeEntry();
            }
        } catch (IOException e) {
            log.error("Failed to create storyboard zip file for workflow: " + workflowId, e);
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

