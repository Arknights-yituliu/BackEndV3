package com.lhs.item;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.entity.dto.item.custom.ItemInfoDTO;
import com.lhs.entity.dto.item.custom.ItemValueConfigDTO;
import com.lhs.service.material.CustomItemService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
public class CustomItemDTOTest {

    @Resource
    private CustomItemService customItemService;

    @Test
    void customItemValueTest(){
        ItemValueConfigDTO itemValueConfigDTO = new ItemValueConfigDTO();
        customItemService.getCustomItemList(itemValueConfigDTO,10);
    }

    @Test
    void formatItemInfoTest(){
        String itemInfoText = FileUtil.read(ConfigUtil.DataFilePath + "item_info.json");

        List<ItemInfoDTO> itemInfoDTOList = JsonMapper.parseJSONArray(itemInfoText, new TypeReference<>() {
        });

        Map<String, ItemInfoDTO> collect = itemInfoDTOList.stream()
                .collect(Collectors.toMap(
                        ItemInfoDTO::getItemId,
                        itemInfoDTO -> itemInfoDTO,
                        (existing, replacement) -> existing  // 保留已存在的值
                ));

        String itemInfoText1 = FileUtil.read(ConfigUtil.DataFilePath + "item_info.v2.json");

        List<ItemInfoDTO> backList = JsonMapper.parseJSONArray(itemInfoText1, new TypeReference<>() {
        });

        for(ItemInfoDTO itemInfoDTO : backList){
            if(collect.containsKey(itemInfoDTO.getItemId())){
                itemInfoDTO.setGroupId(collect.get(itemInfoDTO.getItemId()).getGroupId());
            }
        }



        System.out.println(JsonMapper.toJSONString(collect));


    }
}
