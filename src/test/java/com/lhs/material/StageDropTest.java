package com.lhs.material;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.entity.po.material.StageDrop;
import com.lhs.mapper.material.StageDropMapper;

import jakarta.annotation.Resource;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@SpringBootTest
public class StageDropTest {

    @Resource
    private StageDropMapper stageDropMapper;

    @Test
    public void getActivityDrop() throws Exception {
        List<StageDrop> tmpList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
        Date start = sdf.parse("2025-06-19 00:00:00");
        long oneDay = 1000 * 60 * 60 * 24;
        Date end = new Date(start.getTime() + oneDay);

        for (int i = 0; i < 15; i++) {

            System.out.println("查询日期：" + sdf.format(start) + "至" + sdf.format(end));
            List<StageDrop> list = stageDropMapper.listOldStageDropByDate("stage_drop_20250527_20250808", start, end);

            tmpList = list.stream().filter(e -> "act43side_06".equals(e.getStageId())).toList();
            System.out.println("查询结果数量：" + tmpList.size());
            FileUtil.saveJsonFile("D:\\stageDrop\\AD-6\\", sdf2.format(start) + ".json",
                    JsonMapper.toJSONString(tmpList));
            start.setTime(start.getTime() + oneDay);
            end.setTime(end.getTime() + oneDay);
        }

    }

    @Test
    void importStageDrop() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date start = sdf.parse("2025-05-01 00:00:00");
        long oneDay = 1000 * 60 * 60 * 24;
        Date end = new Date(start.getTime() + oneDay);
        for(int i = 0; i < 100; i++){
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            if (cal.get(Calendar.MONTH) == Calendar.JUNE) {
                System.out.println("start已进入六月，退出循环");
                break;
            }
            
            List<StageDrop> list = stageDropMapper.listOldStageDropByDate("stage_drop_20250422_20250527", start, end);
           
            System.out.println("查询日期：" + sdf.format(start) + "至" + sdf.format(end) + "，数量：" + list.size());
          
            start.setTime(start.getTime() + oneDay);
            end.setTime(end.getTime() + oneDay);
               if(list.isEmpty()){
                continue;
            }
        
            int totalSize = list.size();
            int batchSize = 1000;
            for (int j = 0; j < totalSize; j += batchSize) {
                int endIdx = Math.min(j + batchSize, totalSize);
                List<StageDrop> chunk = list.subList(j, endIdx);
                stageDropMapper.insertBatchByTable("stage_drop_2025_05", chunk);
                System.out.println("已插入 " + chunk.size() + " 条，进度 " + endIdx + "/" + totalSize);
            }
        }
     
    }

}
