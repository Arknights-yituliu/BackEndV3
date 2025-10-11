package com.lhs.item;

import com.lhs.service.material.CustomItemService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CustomItemDTOTest {

    @Resource
    private CustomItemService customItemService;

    @Test
    void customItemValueTest(){
        customItemService.customItemValueCalculation();
    }
}
