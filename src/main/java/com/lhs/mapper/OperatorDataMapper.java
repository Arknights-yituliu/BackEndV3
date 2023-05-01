package com.lhs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.OperatorData;
import com.lhs.entity.OperatorDataVo;
import com.lhs.entity.OperatorStatistics;
import com.lhs.entity.OperatorStatisticsConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OperatorDataMapper extends BaseMapper<OperatorData> {

    List<Long>  selectIdsByPage();

    Integer insertOperatorDataBatch(@Param("tableName") String tableName, @Param("operatorBox")List<OperatorData> operatorBoxList);

    OperatorData selectOperatorDataById(@Param("tableName") String tableName, @Param("id") String id);

    Integer updateOperatorDataById(@Param("tableName") String tableName,@Param("operator") OperatorData operator);

    List<OperatorDataVo> selectOperatorDataByUserId (@Param("tableName") String tableName, @Param("userIds")  List<Long> userIds);
    Integer insertStatisticsBatch(@Param("operatorStatisticsList")List<OperatorStatistics> operatorStatisticsList);

    OperatorStatistics selectStatisticsByCharName(@Param("charName") String charName);

    Integer updateStatisticsByCharName(@Param("statistics") OperatorStatistics statistics);

    OperatorStatisticsConfig selectConfigByKey(@Param("configKey")String configKey);

    Integer updateConfigByKey(@Param("configKey")String configKey,@Param("configValue")String configValue);

    void truncateOperatorStatisticsTable();

    List<OperatorStatistics> selectStatisticsList();
}
