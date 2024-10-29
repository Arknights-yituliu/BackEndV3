package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.rougeSeed.RougeSeedDTO;
import com.lhs.entity.po.admin.LogInfo;
import com.lhs.service.rougeSeed.RougeSeedService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "种子站相关API")
public class RougeSeedController {
    private final RougeSeedService rougeSeedService;

    public RougeSeedController(RougeSeedService rougeSeedService) {
        this.rougeSeedService = rougeSeedService;
    }

    @PostMapping("/auth/rouge-seed/upload")
    public Result<Map<String,Object>> collectLog(@RequestBody RougeSeedDTO rougeSeedDTO, HttpServletRequest httpServletRequest){
        return Result.success(rougeSeedService.saveOrUpdateRougeSeed(rougeSeedDTO,httpServletRequest));
    }
}
