package com.lhs.mapper.material;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.dto.drop.StageDropQuantityCountRawDTO;
import com.lhs.entity.dto.drop.StageDropTimeRangeDTO;
import com.lhs.entity.dto.drop.StageDropTimesCountRawDTO;
import com.lhs.entity.po.material.StageDrop;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;


@Repository
public interface StageDropMapper extends BaseMapper<StageDrop> {

    void insertBatchByTable(@Param("table") String table, @Param("list") List<StageDrop> stageDropList);

    /**
     * 插入单条记录到动态表名
     */
    void insertByTable(@Param("table") String table, @Param("item") StageDrop stageDrop);

    List<StageDrop> listOldStageDropByDate(@Param("tableName") String tableName, @Param("start") Date start, @Param("end") Date end);

    //根据日期查询掉落记录数量，tableName参数拼接的表名，例如：stage_drop_2026_1
    Integer countByDate(@Param("tableName") String tableName,@Param("start") Date start, @Param("end") Date end );

    //根据stageId查询掉落记录
    List<StageDrop> selectListByStageId(@Param("tableName") String tableName, @Param("stageId") String stageId);

    //根据日期查询掉落记录，tableName参数拼接的表名，例如：stage_drop_2026_1
    List<StageDrop> selectListByDate(@Param("tableName") String tableName,@Param("start") Date start, @Param("end") Date end);

    //查询时段的时间范围和记录数，替代 selectListByDate 减少数据传输
    StageDropTimeRangeDTO selectTimeRange(@Param("tableName") String tableName, @Param("start") Date start, @Param("end") Date end);

    // SQL-1：按 stageId 聚合时段内的总 times
    List<StageDropTimesCountRawDTO> selectStageTimesByDate(@Param("tableName") String tableName, @Param("start") Date start, @Param("end") Date end);

    // SQL-2：按 stageId + itemId 聚合掉落数量（JSON_TABLE）
    List<StageDropQuantityCountRawDTO> selectDropStatsByDate(@Param("tableName") String tableName, @Param("start") Date start, @Param("end") Date end);

















    /**
     * 动态表名允许的格式：stage_drop_ 开头，后跟数字和下划线
     * 例如：stage_drop_2025_01
     */
    Pattern TABLE_NAME_PATTERN = Pattern.compile("^stage_drop_\\d{4}_\\d{2}$");

    /**
     * 校验动态表名，防止 SQL 注入
     *
     * @param tableName 动态表名
     * @throws IllegalArgumentException 表名不合法时抛出
     */
    default void validateTableName(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("表名不能为空");
        }
        if (!TABLE_NAME_PATTERN.matcher(tableName).matches()) {
            throw new IllegalArgumentException(
                    "表名格式不合法，仅允许 stage_drop_YYYY_MM 格式，实际值: " + tableName);
        }
    }

}
