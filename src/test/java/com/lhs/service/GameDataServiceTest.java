package com.lhs.service;

import com.lhs.service.util.impl.ArknightsGameDataServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 一般用于术语生成
 * 使用Mock避免启动整个Spring容器
 */
@ExtendWith(MockitoExtension.class)
public class GameDataServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(GameDataServiceTest.class);

    @InjectMocks
    private ArknightsGameDataServiceImpl arknightsGameDataService;

    /**
     * 术语生成
     */
    @Test
    void testGetTermDescriptionTable() {
        try {
            // 使用测试路径，无需传入任何路径
            arknightsGameDataService.getTermDescriptionTable(null);
            logger.info("Term description table generated successfully");
        } catch (Exception e) {
            System.err.println("Test failed with error: " + e.getMessage());
            logger.error("Test failed while generating term description table", e);
            throw e;
        }
    }
}

