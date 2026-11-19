package com.lhs.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.lhs.BackEndApplication;
import com.lhs.entity.dto.material.ItemValueConfigDTO;
import com.lhs.entity.vo.material.HistoryActStageVO;
import com.lhs.entity.vo.material.RecommendedStageVO;
import com.lhs.entity.vo.material.StageEfficiencyResultVO;
import com.lhs.entity.vo.material.StageEfficiencyVO;
import com.lhs.service.material.StageEfficiencyService;

import jakarta.annotation.Resource;

/**
 * 材料推荐关卡（v7）测试类
 * 覆盖 /stage/v7/recommended 的核心计算逻辑；资源路径覆盖为本地 resources，避免依赖线上部署目录
 */
@SpringBootTest(classes = BackEndApplication.class, properties = {
        "resourcesPath.penguin=src/main/resources/penguin/",
        "resourcesPath.config=src/main/resources/config/",
        "resourcesPath.data=src/main/resources/data/",
        "resourcesPath.backup=src/main/resources/backup/"
})
public class RecommendedStageAPITest {

    @Resource
    private StageEfficiencyService stageEfficiencyService;

    /**
     * 使用默认定价配置计算推荐关卡，校验推荐结果的分组、排序与开放状态
     */
    @Test
    void getRecommendedStage() {
        ItemValueConfigDTO config = new ItemValueConfigDTO();
        StageEfficiencyResultVO result = stageEfficiencyService.getRecommendedStage(config);

        assertNotNull(result, "推荐关卡结果不应为 null");
        List<RecommendedStageVO> recommendedStageList = result.getRecommendedStage();
        assertNotNull(recommendedStageList, "材料系列推荐列表不应为 null");
        assertFalse(recommendedStageList.isEmpty(), "材料系列推荐列表不应为空");

        long now = System.currentTimeMillis();
        System.out.println("========== 材料推荐关卡（v7）==========");
        System.out.println("推荐材料系列数：" + recommendedStageList.size());
        for (RecommendedStageVO series : recommendedStageList) {
            assertNotNull(series.getItemSeriesId(), "材料系列ID不应为 null");
            List<StageEfficiencyVO> stageList = series.getStageResultList();
            assertFalse(stageList.isEmpty(), "材料系列 " + series.getItemSeriesId() + " 的推荐关卡不应为空");
            // 推荐关卡只包含仍开放的关卡
            for (StageEfficiencyVO stage : stageList) {
                assertNotNull(stage.getEnd(), "关卡 " + stage.getStageCode() + " 的结束时间不应为 null");
                assertTrue(stage.getEnd() > now, "关卡 " + stage.getStageCode() + " 已结束却出现在推荐列表");
                // 材料系列推荐关卡不返回掉落明细
                assertNull(stage.getDropDetail(), "关卡 " + stage.getStageCode() + " 不应返回掉落明细");
            }
            assertStageEfficiencyDesc(stageList);

            System.out.printf("系列 %s（共 %d 个推荐关卡）：%n", series.getItemSeriesId(), stageList.size());
            // 每个材料系列最多打印前 3 个推荐关卡
            for (int i = 0; i < Math.min(stageList.size(), 3); i++) {
                StageEfficiencyVO stage = stageList.get(i);
                System.out.printf("  %d. 关卡 %s（%s / %s），理智转化率 %.4f，单件期望理智 %.2f%n",
                        i + 1, stage.getStageCode(), stage.getItemName(), stage.getZoneName(),
                        stage.getStageEfficiency(), stage.getApExpect());
            }
        }

        List<StageEfficiencyVO> orundumStageList = result.getOrundumRecommendedStageVO();
        assertNotNull(orundumStageList, "搓玉推荐关卡列表不应为 null");
        assertOrundumPerApDesc(orundumStageList);
        System.out.println("搓玉推荐关卡数：" + orundumStageList.size());
        for (int i = 0; i < Math.min(orundumStageList.size(), 5); i++) {
            StageEfficiencyVO stage = orundumStageList.get(i);
            System.out.printf("搓玉推荐 %s（%s），每理智合成玉 %.4f，相对效率 %.4f%n",
                    stage.getStageCode(), stage.getZoneName(), stage.getOrundumPerAp(),
                    stage.getOrundumPerApEfficiency());
        }
        if (!orundumStageList.isEmpty()) {
            assertEquals(1.0, orundumStageList.get(0).getOrundumPerApEfficiency(), 1e-9,
                    "搓玉推荐榜首的相对效率应为 1");
        }

        List<HistoryActStageVO> historyActStageList = result.getHistoryActStage();
        assertNotNull(historyActStageList, "历史活动关卡列表不应为 null");
        System.out.println("历史活动关卡活动数：" + historyActStageList.size());
        for (HistoryActStageVO historyActStage : historyActStageList) {
            assertNotNull(historyActStage.getZoneName(), "历史活动名称不应为 null");
            assertFalse(historyActStage.getActStageList().isEmpty(),
                    "历史活动 " + historyActStage.getZoneName() + " 的关卡列表不应为空");
        }

        List<HistoryActStageVO> historyActOrundumStageList = result.getHistoryActOrundumStage();
        assertNotNull(historyActOrundumStageList, "历史活动搓玉关卡列表不应为 null");
        System.out.println("历史活动搓玉关卡活动数：" + historyActOrundumStageList.size());
    }

    /**
     * 校验关卡列表按理智转化率降序排列
     *
     * @param stageList 关卡结果列表
     */
    private void assertStageEfficiencyDesc(List<StageEfficiencyVO> stageList) {
        for (int i = 1; i < stageList.size(); i++) {
            Double previous = stageList.get(i - 1).getStageEfficiency();
            Double current = stageList.get(i).getStageEfficiency();
            assertNotNull(previous, "关卡 " + stageList.get(i - 1).getStageCode() + " 的理智转化率不应为 null");
            assertNotNull(current, "关卡 " + stageList.get(i).getStageCode() + " 的理智转化率不应为 null");
            assertTrue(previous >= current, "关卡列表未按理智转化率降序排列：" + previous + " -> " + current);
        }
    }

    /**
     * 校验搓玉关卡列表按每理智合成玉产出降序排列
     *
     * @param stageList 搓玉关卡结果列表
     */
    private void assertOrundumPerApDesc(List<StageEfficiencyVO> stageList) {
        for (int i = 1; i < stageList.size(); i++) {
            Double previous = stageList.get(i - 1).getOrundumPerAp();
            Double current = stageList.get(i).getOrundumPerAp();
            assertNotNull(previous, "关卡 " + stageList.get(i - 1).getStageCode() + " 的每理智合成玉不应为 null");
            assertNotNull(current, "关卡 " + stageList.get(i).getStageCode() + " 的每理智合成玉不应为 null");
            assertTrue(previous >= current, "搓玉关卡列表未按每理智合成玉降序排列：" + previous + " -> " + current);
        }
    }
}
