package com.taichu.gateway.web.user;


import com.alibaba.cola.dto.SingleResponse;
import com.taichu.gateway.model.WorkflowDTO;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@Api(tags = "用户接口")
public class UserController {

    @GetMapping("get-active-workflow")
    public SingleResponse<WorkflowDTO> getWorkflow(@RequestParam Long userId) {
        return SingleResponse.buildSuccess();
    }
}
