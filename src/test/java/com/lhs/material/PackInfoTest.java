package com.lhs.material;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.entity.po.material.PackInfo;
import com.lhs.entity.vo.material.PackContentVO;
import com.lhs.mapper.material.PackInfoMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;

@SpringBootTest
public class PackInfoTest {

    @Resource
    private PackInfoMapper packInfoMapper;

    @Test
    void updatePackItem() {
        HashMap<String, String> hashMap = new HashMap<>();

        String read = FileUtil.read(ConfigUtil.DataFilePath + "custom_item_info.json");
        JsonNode customItemInfoNode = JsonMapper.parseObject(read, new TypeReference<>() {
        });

        for(JsonNode jsonNode :customItemInfoNode){
            hashMap.put(jsonNode.get("itemName").asText(), jsonNode.get("itemId").asText());
        }

        hashMap.put("1934834757761000", "voucher_levelmax_4");
        hashMap.put("1920059628821000", "business_card_theme");
        hashMap.put("1890773841491000", "voucher_skin");
        hashMap.put("1890770850421000", "mod_update_token_1");
        hashMap.put("1890770784181000", "mod_update_token_2");
        hashMap.put("1882111375581000", "business_card_theme");
        hashMap.put("1882111254381000", "business_card_theme");
        hashMap.put("1882110981381000", "business_card_theme");
        hashMap.put("1882110727181000", "business_card_theme");
        hashMap.put("1882110416581000", "business_card_theme");
        hashMap.put("1882109349571000", "avatar_special");
        hashMap.put("1882109116601000", "avatar_special");
        hashMap.put("1873450981701000", "itempack_main");
        hashMap.put("1817326660760100", "business_card_theme");
        hashMap.put("1733141049220100", "voucher_item_pick6");
        hashMap.put("1733141121810100", "voucher_class_pick6");
        hashMap.put("1681763124760100", "avatar_special");
        hashMap.put("1681763062980100", "avatar_special");
        hashMap.put("1681762997530100", "avatar_special");
        hashMap.put("1681762879580100", "avatar_special");
        hashMap.put("1681592234490100", "business_card_theme");
        hashMap.put("1656359200980100", "interface_theme");
        hashMap.put("1656359153030100", "theme_scene");
        hashMap.put("1656359027460100", "avatar_special");
        hashMap.put("1656358975290100", "business_card_theme");
        hashMap.put("1654447423900100", "voucher_skill_specialLevelMax_4");
        hashMap.put("1654442784200100", "voucher_skill_specialLevelMax_5");
        hashMap.put("1654442019480100", "voucher_skill_specialLevelMax_6");
        hashMap.put("1646835672680100", "business_card_theme");
        hashMap.put("1646835592640100", "business_card_theme");
        hashMap.put("1646835466590100", "business_card_theme");
        hashMap.put("1646835259720100", "business_card_theme");
        hashMap.put("1633694012570100", "voucher_elite_II_4");
        hashMap.put("1617732742670100", "voucher_5chipPackage");
        hashMap.put("1617731551750100", "advanced_material_issue_voucher");
        hashMap.put("1617731244580100", "premium_material_issue_voucher");
        hashMap.put("1525119363050100", "voucher_elite_II_6");
        hashMap.put("1525115679550100", "interface_theme");
        hashMap.put("1525115616270100", "theme_scene");
        hashMap.put("1525106667340100", "avatar_special");
        hashMap.put("1521562891830100", "voucher_levelmax_6");
        hashMap.put("1521562703490100", "voucher_levelmax_5");
        hashMap.put("1521560042260100", "voucher_skin");
        hashMap.put("1501659232460100", "voucher_elite_II_5");
        hashMap.put("1500990089550100", "voucher_elite_II_5");
        hashMap.put("1499354830891000", "ap_supply_lt_80");
        hashMap.put("1499353372271000", "voucher_chipPackage");


        List<PackInfo> packInfoList = packInfoMapper.selectList(null);
        for (PackInfo packInfo : packInfoList) {
//            System.out.println(packInfo.getContent());
            if (packInfo.getContent() != null) {
                List<PackContentVO> packContentVOList = JsonMapper.parseObject(packInfo.getContent(), new TypeReference<>() {
                });

                for (PackContentVO packContentVO : packContentVOList) {
                    if (hashMap.containsKey(packContentVO.getItemId())) {
                        packContentVO.setItemId(hashMap.get(packContentVO.getItemId()));
                        continue;
                    }

                    if (hashMap.containsKey(packContentVO.getItemName())) {
                        packContentVO.setItemId(hashMap.get(packContentVO.getItemName()));
                        continue;
                    }


                    if (packContentVO.getItemName().contains("庆典干员凭证")) {
                        packContentVO.setItemId("voucher_item_pick6");
                        packContentVO.setItemName("高级干员调用凭证（周年庆典干员凭证）");
                        continue;
                    }

                    if (packContentVO.getItemName().contains("高级干员调用凭证")) {
                        packContentVO.setItemId("voucher_item_pick6");
                        packContentVO.setItemName("高级干员调用凭证（周年庆典干员凭证）");
                        continue;
                    }

                    if (packContentVO.getItemName().contains("新年寻访凭证")) {
                        packContentVO.setItemId("recruitment10");
                        packContentVO.setItemName("新年寻访凭证");
                        continue;
                    }


                    if (packContentVO.getItemName().contains("家具零件")) {
                        packContentVO.setItemId("3401");
                        continue;
                    }



                    if (packContentVO.getItemName().contains("中坚高级干员调用凭证")) {
                        packContentVO.setItemId("voucher_class_pick6");
                        packContentVO.setItemName("中坚高级干员调用凭证");
                        continue;
                    }


                    System.out.println("没有这个材料——" + packContentVO.getItemId() + "————————" + packContentVO.getItemName());
                }

                packInfo.setContent(JsonMapper.toJSONString(packContentVOList));
            }


           packInfoMapper.updateById(packInfo);

        }

    }
}
