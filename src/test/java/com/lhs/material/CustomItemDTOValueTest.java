package com.lhs.material;

import com.lhs.entity.dto.item.custom.ItemValueConfigDTO;
import com.lhs.service.material.CustomItemService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CustomItemDTOValueTest {

    @Resource
    private CustomItemService customItemService;

    @Test
    void itemValueConfigTest(){
        new ItemValueConfigDTO();
    }

    @Test
    void customItemValueCalculation(){
        customItemService.getCustomItemList();
    }
}
