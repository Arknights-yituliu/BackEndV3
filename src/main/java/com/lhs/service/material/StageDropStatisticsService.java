package com.lhs.service.material;

import com.lhs.common.util.IdGenerator;

import com.lhs.mapper.material.StageDropMapper;
import com.lhs.mapper.material.StageDropStatisticsMapper;
import org.springframework.stereotype.Service;

@Service
public class StageDropStatisticsService {

    private final StageDropMapper stageDropMapper;

    private final StageDropStatisticsMapper stageDropStatisticsMapper;

    private final IdGenerator idGenerator;

    public StageDropStatisticsService(StageDropMapper stageDropMapper,
            StageDropStatisticsMapper stageDropStatisticsMapper) {
        this.stageDropMapper = stageDropMapper;
        this.stageDropStatisticsMapper = stageDropStatisticsMapper;
        this.idGenerator = new IdGenerator(6L);

    }


    

}
