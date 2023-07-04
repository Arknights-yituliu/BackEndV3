package com.lhs.service.stage;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.HttpRequestUtil;
import com.lhs.common.util.OssUtil;
import com.lhs.mapper.StageMapper;
import com.lhs.entity.stage.Item;
import com.lhs.entity.stage.Stage;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StageService extends ServiceImpl<StageMapper, Stage>  {

    @Resource
    private StageMapper stageMapper;
    @Resource
    private ItemService itemService;


    /**
     * 保存企鹅物流数据到本地
     */
    public void savePenguinData() {
        String penguinGlobal = ApplicationConfig.PenguinGlobal;
        String penguinAuto = ApplicationConfig.PenguinAuto;
        String responseAuto = HttpRequestUtil.doGet(penguinAuto, new HashMap<>());
        String responseGlobal = HttpRequestUtil.doGet(penguinGlobal, new HashMap<>());
        String yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 设置日期格式
        String yyyyMMddHHmm = new SimpleDateFormat("yyyy-MM-dd hh:mm").format(new Date()); // 设置日期格式
        if (responseAuto == null||responseGlobal==null) return;

        FileUtil.save(ApplicationConfig.Penguin, "matrix auto.json", responseAuto);
        OssUtil.upload(responseAuto,"penguin/" + yyyyMMdd + "/matrix auto " + yyyyMMddHHmm + ".json");

        FileUtil.save(ApplicationConfig.Penguin, "matrix global.json", responseGlobal);
        OssUtil.upload(responseGlobal,"penguin/" + yyyyMMdd + "/matrix global " + yyyyMMddHHmm + ".json");
    }



    public List<Stage> getStageTable(QueryWrapper<Stage> queryWrapper) {

        return stageMapper.selectList(queryWrapper);

    }


    @Transactional
    public void importStageData(MultipartFile file) {
        List<Stage> list = new ArrayList<>();

        Map<String, Item> itemMap = itemService.queryItemList(0.625).stream().collect(Collectors.toMap(Item::getItemName, Function.identity()));
        JSONObject itemType_table = JSONObject.parseObject(FileUtil.read(ApplicationConfig.Item + "itemType_table.json"));
//        JSONObject stageZone_table = JSONObject.parseObject(FileUtil.read(FileConfig.Item + "zone_table.json"));

        try {
            EasyExcel.read(file.getInputStream(), Stage.class, new AnalysisEventListener<Stage>() {

                public void invoke(Stage stage, AnalysisContext analysisContext) {
                    try {
                        if (!"0".equals(stage.getMain())) stage.setMainRarity(itemMap.get(stage.getMain()).getRarity());
                        if (!"0".equals(stage.getMain())) stage.setItemType(itemType_table.getString(stage.getMain()));

//                        JSONObject zone = JSONObject.parseObject(stageZone_table.getString(stage.getZoneId()));
//                        stage.setZoneName(zone.getString("zoneName"));
//                        if(zone.get("openTime")==null){
//                            stage.setOpenTime( new Date(1556676029000L));
//                        }else {
//                            stage.setOpenTime(new Date(Long.parseLong(zone.getString("openTime"))));
//                        }

                        stage.setSpm(stage.getApCost() / stage.getMinClearTime() * 60000);
                        if (!"0".equals(stage.getSecondary()))
                            stage.setSecondaryId(itemMap.get(stage.getSecondary()).getItemId());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    if (stage.getZoneId().contains("act")) {
                        stage.setStageState(1);
                        stage.setIsValue(0);
                        stage.setIsShow(0);
                        if (stage.getZoneId().contains("perm")) {
                            stage.setIsShow(1);
                            stage.setIsValue(1);
                        }
                    } else {
                        stage.setIsValue(1);
                        stage.setIsShow(1);
                        stage.setStageState(0);
                    }
                    list.add(stage);
                }
                public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                }
            }).sheet().doRead();

        } catch (IOException e) {
            e.printStackTrace();
        }

        saveOrUpdateBatch(list);

    }


   
    public void exportStageData(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("stages", "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

            List<Stage> list = stageMapper.selectList(null);

            EasyExcel.write(response.getOutputStream(), Stage.class).sheet("Sheet1").doWrite(list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

   
    public Integer updateStageInfo(Stage stage) {
        Integer isShow = stage.getIsShow()==1?0:1;
        stage.setIsShow(isShow);
        int update = stageMapper.update(stage,new QueryWrapper<Stage>().eq("stage_id",stage.getStageId()));
        return isShow;
    }

    public LinkedHashMap<String, List<Stage>> queryStageTable(){
        List<Stage> stageList = stageMapper.selectList(new QueryWrapper<Stage>().notLike("stage_id","tough").orderByDesc("stage_id"));
        List<Stage> zoneList = stageMapper.selectList(new QueryWrapper<Stage>().notLike("stage_id","tough").groupBy("zone_id").orderByDesc("stage_id"));
        Map<String, List<Stage>> collect = stageList.stream().collect(Collectors.groupingBy(Stage::getZoneName));
        LinkedHashMap<String, List<Stage>> result = new LinkedHashMap<>();
        zoneList.forEach(stage->result.put(stage.getZoneName(),collect.get(stage.getZoneName())));

        return result;
    }





}
