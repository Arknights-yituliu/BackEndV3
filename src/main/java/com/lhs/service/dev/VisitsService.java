package com.lhs.service.dev;

import com.lhs.entity.vo.dev.PageVisitsVo;
import com.lhs.entity.vo.dev.VisitsTimeVo;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

public interface VisitsService {

    void updatePageVisits(String path);

    @Scheduled(cron = "0 0/17 * * * ?")
    void savePageVisits();

    List<PageVisitsVo> getVisits(VisitsTimeVo visitsTimeVo);
}
