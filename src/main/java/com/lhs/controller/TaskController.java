package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.task.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Tag(name = "A后台更新")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary ="调查站用户注册")
    @GetMapping("/update/stage")
    public Result<Object> updateStage(){
        taskService.updateStageResult();
        return Result.success();
    }

    @Operation(summary ="调查站用户注册")
    @GetMapping("/update/pack")
    public Result<Object> updateStorePack(){

        return Result.success();
    }
}
