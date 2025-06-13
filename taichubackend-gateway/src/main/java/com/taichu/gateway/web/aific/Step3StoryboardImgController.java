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
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        return SingleResponse.buildSuccess();
    }

    @GetMapping("/download")
    @ApiOperation(value = "下载分镜", notes = "下载所有分镜压缩包")
    public ResponseEntity<Resource> downloadStoryboardZip(@RequestBody Long workflowId) {
        // 下载分镜压缩包
        return ResponseEntity.ok().build();
    }
}

