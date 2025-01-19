package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.rogue.*;
import com.lhs.entity.po.rogue.RogueSeedRating;
import com.lhs.entity.vo.rogue.RogueSeedVO;

import com.lhs.service.rogue.RogueSeedService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping("/rogue/seed/upload")
    public Result<Map<String,Object>> uploadSeed(@RequestBody RogueSeedDTO rogueSeedDTO, HttpServletRequest httpServletRequest){
        return Result.success(rogueSeedService.saveOrUpdateRogueSeed(httpServletRequest,rogueSeedDTO));
    }

    @PostMapping("/auth/rogue/seed/settlement-chart")
    public Result<Map<String,Object>> uploadSeedSettlementChart(@RequestParam("file") MultipartFile multipartFile, HttpServletRequest httpServletRequest){
        return Result.success(rogueSeedService.uploadSettlementChart(multipartFile,httpServletRequest));
    }

    @PostMapping("/auth/rogue/seed/rating")
    public Result<String> rating(@RequestBody RogueSeedRatingDTO rogueSeedRatingDTO, HttpServletRequest httpServletRequest){

        return Result.success(rogueSeedService.rogueSeedRating(rogueSeedRatingDTO,httpServletRequest));
    }

    @PostMapping("/auth/rogue/seed/record-action")
    public Result<String> rating(@RequestBody UserActionOnSeedDTO userActionOnSeedDTO, HttpServletRequest httpServletRequest){
        rogueSeedService.recordUserActionOnSeed(userActionOnSeedDTO,httpServletRequest);
        return Result.success();
    }

    @GetMapping("/rogue/seed/user/rating")
    public Result<Map<Long, RogueSeedRating>> ratingList(HttpServletRequest httpServletRequest){
        return Result.success(rogueSeedService.listRogueSeedUserRating(httpServletRequest));
    }



    @PostMapping("/rogue/seed/page")
    public Result<List<RogueSeedVO>> getSeedPage(@RequestBody RogueSeedPageDTO rogueSeedPageDTO, HttpServletRequest httpServletRequest){
        return Result.success(rogueSeedService.listRogueSeed(rogueSeedPageDTO,httpServletRequest));
    }

    @PostMapping("/rogue/seed/roll")
    public Result<RogueSeedVO> rollSeed(@RequestBody RollRogueSeedDTO rogueSeedDTO, HttpServletRequest httpServletRequest){
        return Result.success(rogueSeedService.rollRogueSeed(rogueSeedDTO,httpServletRequest));
    }


}
