package com.lhs.service.material;

import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.entity.dto.material.QueryStageDropDTO;
import com.lhs.entity.po.material.StageDropStatistics;
import com.lhs.entity.vo.material.StageDropStatisticsVO;
import com.lhs.mapper.material.StageDropMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class StageDropService {

    private final StageDropMapper stageDropMapper;

    public StageDropService(StageDropMapper stageDropMapper) {
        this.stageDropMapper = stageDropMapper;
    }

    public List<StageDropStatisticsVO> getStageDropByStageId(QueryStageDropDTO queryStageDropDTO) {
        List<StageDropStatistics> stageDropStatisticsList = stageDropMapper.
                listStageDropStatisticsByStageId(queryStageDropDTO.getStageId(), queryStageDropDTO.getTimeGranularity(),
                        new Date(queryStageDropDTO.getStart()), new Date(queryStageDropDTO.getEnd()));

        if (stageDropStatisticsList.isEmpty()) {
            throw new ServiceException(ResultCode.DATA_NONE);
        }

        List<StageDropStatisticsVO> voList = new ArrayList<>();
        for (StageDropStatistics po : stageDropStatisticsList) {
            StageDropStatisticsVO stageDropStatisticsVO = new StageDropStatisticsVO();
            stageDropStatisticsVO.setStageId(po.getStageId());
            stageDropStatisticsVO.setItemId(po.getItemId());
            stageDropStatisticsVO.setTimes(po.getTimes());
            stageDropStatisticsVO.setQuantity(po.getQuantity());
            stageDropStatisticsVO.setStart(po.getStart().getTime());
            stageDropStatisticsVO.setEnd(po.getEnd().getTime());
            voList.add(stageDropStatisticsVO);
        }
        return voList;
    }


    private void setStartAndEnd(StageDropStatisticsVO vo,Date period,Integer timeGranularity){
        if(timeGranularity==2){
           vo.setStart(period.getTime()-60*60*1000);
           vo.setEnd(period.getTime()+60*60);
        }


        vo.setStart(period.getTime() - 60 * 60 * 1000);
        vo.setEnd(period.getTime());
    }
}
