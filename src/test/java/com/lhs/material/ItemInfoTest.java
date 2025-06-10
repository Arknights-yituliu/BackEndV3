package com.lhs.material;

import com.lhs.entity.po.material.ItemInfo;
import com.lhs.mapper.material.ItemInfoMapper;
import com.lhs.mapper.material.ItemMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ItemInfoTest {

    @Resource
    private ItemInfoMapper itemInfoMapper;
    @Resource
    private ItemMapper itemMapper;

    @Test
    void importItemInfo(){



    }
}
