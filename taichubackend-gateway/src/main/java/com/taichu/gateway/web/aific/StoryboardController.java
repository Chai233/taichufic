package com.taichu.gateway.web.aific;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.service.StoryboardAppService;
import com.taichu.sdk.model.request.GenerateStoryboardRequest;
import com.taichu.sdk.model.request.SingleStoryboardRegenRequest;
import com.taichu.sdk.model.StoryboardImgListItemDTO;
import com.taichu.sdk.model.StoryboardTaskStatusDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/storyboard")
@Api(tags = "Page 3 - 分镜图接口")
public class StoryboardController {

    private final StoryboardAppService storyboardAppService;

    public StoryboardController(StoryboardAppService storyboardAppService) {
        this.storyboardAppService = storyboardAppService;
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
        return storyboardAppService.generateStoryboard(request, null);
    }

    @GetMapping("/task/status")
    @ApiOperation(value = "获取任务进度", notes = "")
    public SingleResponse<StoryboardTaskStatusDTO> getStoryboardTaskStatus(@RequestParam("taskId") Long taskId) {
        // 轮询任务结果
        return SingleResponse.buildSuccess();
    }

    @GetMapping("/getAll")
    @ApiOperation(value = "获取全部分镜信息", notes = "")
    public MultiResponse<StoryboardImgListItemDTO> getAllStoryboard(@RequestParam Long workflowId) {
        // 获取分镜列表
        return MultiResponse.buildSuccess();
    }

    @GetMapping("/getSingle")
    @ApiOperation(value = "获取单个分镜信息", notes = "")
    public SingleResponse<StoryboardImgListItemDTO> getSingleStoryboard(@RequestParam Long workflowId, @RequestParam Long storyboardId) {
        // 获取分镜列表
        return SingleResponse.buildSuccess();
    }

    @PostMapping("/regenerate")
    @ApiOperation(value = "分镜图重新生成", notes = "修改单张分镜。返回修改任务id")
    public SingleResponse<Long> regenerateSingleStoryboard(@RequestBody SingleStoryboardRegenRequest request) {
        // 单张分镜重新生成
        return SingleResponse.buildSuccess();
    }

    @GetMapping("/download")
    @ApiOperation(value = "下载分镜", notes = "下载所有分镜压缩包")
    public ResponseEntity<Resource> downloadStoryboardZip(@RequestBody Long workflowId) {
        // 下载分镜压缩包
        return ResponseEntity.ok().build();
    }
}

