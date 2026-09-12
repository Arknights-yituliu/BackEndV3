package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.survey.OperatorProgressionData;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface OperatorProgressionDataMapper extends BaseMapper<OperatorProgressionData> {

    List<OperatorProgressionData> getOperatorProgressionDataByDate(@Param("createTime") Date createTime, @Param("offset") Integer offset);

    /**
     * 按主键 ak_uid 游标分页查询干员练度数据
     *
     * @param lastAkUid 上一批最后一条的 ak_uid，首次传空字符串
     * @param rowNum    每批查询条数
     * @return 干员练度数据列表
     */
    List<OperatorProgressionData> getOperatorProgressionData(@Param("lastAkUid") String lastAkUid, @Param("rowNum") Integer rowNum);
}
