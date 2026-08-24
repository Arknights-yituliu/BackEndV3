package com.lhs.mapper.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.dto.UrlCountDTO;
import com.lhs.entity.po.admin.AccessLog;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * 访问日志Mapper接口
 * 对应数据库表：access_log
 */
@Repository
public interface AccessLogMapper extends BaseMapper<AccessLog> {

    /**
     * 统计指定时间范围内的页面浏览量(PV)
     */
    Long countPageViews(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 统计指定时间范围内的独立访客数(UV)
     */
    Long countUniqueVisitors(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 按URL分组统计指定时间范围内各URL的访问量
     */
    List<UrlCountDTO> countByUrl(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
}
