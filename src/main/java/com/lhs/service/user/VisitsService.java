package com.lhs.service.user;

import com.lhs.entity.vo.dev.PageViewStatisticsVo;
import com.lhs.entity.vo.dev.VisitsTimeVo;

import java.util.List;

public interface VisitsService {

    void updatePageVisits(String path);


    void savePageVisits();

    List<PageViewStatisticsVo> getVisits(VisitsTimeVo visitsTimeVo);
}
