package com.lhs.drop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;

import com.lhs.entity.dto.drop.StageDropQuantityCountRawDTO;
import com.lhs.entity.dto.drop.StageDropTimesCountRawDTO;
import com.lhs.entity.po.material.StageDrop;
import com.lhs.entity.vo.drop.StageDropStatisticsResultVO;
import com.lhs.mapper.material.StageDropMapper;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class StageDropExportTest {

    @Resource
    private StageDropMapper stageDropMapper;

    /**
     * 读取导出的 JSON 文件，按 stageId 查看指定关卡的掉率明细
     */
    @Test
    public void readExportedDropByStageId() {
        List<StageDrop> list = stageDropMapper.selectListByStageId("stage_drop_2025_06", "act43side_06");
        System.out.println(list.size());
         FileUtil.saveJsonFile("D:\\stageDrop\\export\\", "act43side_06.json",
                JsonMapper.toJSONString(list));
    }
}
