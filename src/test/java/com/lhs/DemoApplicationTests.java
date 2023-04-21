package com.lhs;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.config.FileConfig;
import com.lhs.common.util.FileUtil;
import com.lhs.entity.*;
import com.lhs.mapper.*;
import com.lhs.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@SpringBootTest
class DemoApplicationTests {


    @Resource
    private StageService stageService;

    @Test
    void readGameData() {
         stageService.readGameData_stageFile();
    }



}
