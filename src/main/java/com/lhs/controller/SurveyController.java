package com.lhs.controller;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.FileConfig;
import com.lhs.common.util.IpUtil;
import com.lhs.common.util.Result;
import com.lhs.service.OperatorSurveyService;
import com.lhs.service.RecruitSurveyService;
import com.lhs.service.dto.MaaOperBoxVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

@RestController
@Api(tags = "调查接口")
@RequestMapping(value = "/survey")
@CrossOrigin(maxAge = 86400)
public class SurveyController {

    @Resource
    private OperatorSurveyService operatorSurveyService;



}
