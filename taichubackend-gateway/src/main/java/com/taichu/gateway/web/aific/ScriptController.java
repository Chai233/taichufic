package com.taichu.gateway.web.aific;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.taichu.application.service.ScriptAppService;
import com.taichu.sdk.model.GenerateScriptRequest;
import com.taichu.sdk.model.ScriptDTO;
import com.taichu.sdk.model.TaskStatusDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/script")
@Api(tags = "Page 2 - 剧本接口")
public class ScriptController {

    @Autowired
    private ScriptAppService scriptAppService;

    @PostMapping("/generate")
    @ApiOperation(value = "提交剧本生成任务", notes = "")
    public SingleResponse<Long> generateScript(@RequestBody GenerateScriptRequest request) {
        // 提交剧本生成任务
        // TODO@chai 获取当前登录用户
        return scriptAppService.submitGenScriptTask(request, null);
    }

    @GetMapping("/task/status")
    @ApiOperation(value = "查询任务状态", notes = "")
    public SingleResponse<TaskStatusDTO> getScriptTaskStatus(@RequestParam("workflow_id") Long workflowId) {
        // 轮询任务结果
        return scriptAppService.getScriptTaskStatus(workflowId);
    }

    @GetMapping("/getScript")
    @ApiOperation(value = "获取剧本", notes = "")
    public MultiResponse<ScriptDTO> getScript(@RequestParam Long workflowId) {
        // 获取剧本
        return MultiResponse.buildSuccess();
    }

    @GetMapping("/downloadScript")
    @ApiOperation(value = "下载剧本", notes = "")
    public ResponseEntity<Resource> downloadScript(@RequestParam Long workflowId) {
        // 下载剧本
        return ResponseEntity.ok().build();
    }
}

