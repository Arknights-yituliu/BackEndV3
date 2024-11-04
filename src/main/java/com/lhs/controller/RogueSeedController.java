package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.rogueSeed.RogueSeedDTO;
import com.lhs.entity.dto.rogueSeed.RogueSeedPageRequest;
import com.lhs.entity.vo.rogueSeed.RogueSeedPageVO;
import com.lhs.service.rogueSeed.RogueSeedService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "种子站相关API")
public class RogueSeedController {
    private final RogueSeedService rogueSeedService;

    public RogueSeedController(RogueSeedService rogueSeedService) {
        this.rogueSeedService = rogueSeedService;
    }

    @PostMapping("/auth/rogue-seed/upload")
    public Result<Map<String,Object>> uploadSeed(@RequestBody RogueSeedDTO rogueSeedDTO, HttpServletRequest httpServletRequest){
        return Result.success(rogueSeedService.saveOrUpdateRogueSeed(rogueSeedDTO,httpServletRequest));
    }

    @PostMapping("/auth/rogue-seed/settlement-chart")
    public Result<Map<String,Object>> uploadSeedSettlementChart(@RequestParam("file") MultipartFile multipartFile, HttpServletRequest httpServletRequest){
        return Result.success(rogueSeedService.uploadSettlementChart(multipartFile,httpServletRequest));
    }

    @PostMapping("/auth/rogue-seed/rating")
    public Result<Map<String,Object>> rating(@RequestParam Double rating, HttpServletRequest httpServletRequest){
        return Result.success(rogueSeedService.rogueSeedRating(rating,httpServletRequest));
    }


    @PostMapping("/auth/rogue-seed/page")
    public Result<List<RogueSeedPageVO>> seedPage(@RequestBody RogueSeedPageRequest rogueSeedDTO){
        return Result.success(rogueSeedService.listRogueSeed(rogueSeedDTO));
    }
}
