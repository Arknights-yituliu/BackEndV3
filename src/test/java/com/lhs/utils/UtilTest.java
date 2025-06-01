package com.lhs.utils;

import com.lhs.common.util.TimeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UtilTest {

    @Test
    void timeUtil(){
        String dayText = TimeUtil.getDayText();
    }
}
