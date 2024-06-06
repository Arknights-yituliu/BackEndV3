package com.lhs.service.dev.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.Logger;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.po.dev.PageViewStatistics;
import com.lhs.entity.vo.dev.PageViewStatisticsVo;
import com.lhs.entity.vo.dev.VisitsTimeVo;
import com.lhs.mapper.dev.PageVisitsMapper;
import com.lhs.service.dev.VisitsService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VisitsServiceImpl implements VisitsService {


    private final PageVisitsMapper pageVisitsMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    public VisitsServiceImpl(PageVisitsMapper pageVisitsMapper, RedisTemplate<String, Object> redisTemplate) {
        this.pageVisitsMapper = pageVisitsMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void updatePageVisits(String path) {
        if (path == null || path.length() > 40) return;
        String format = new SimpleDateFormat("yyyy/MM/dd HH").format(new Date());
        path = path.replace("/src", "");
        path = path.replace("/pages", "");

        String redisKey = format + "." + path;

        redisTemplate.opsForHash().increment("visits", redisKey, 1);
    }

    @Scheduled(cron = "0 0/17 * * * ?")
    @Override
    public void savePageVisits() {
        Date todayDate = new Date();
        Logger.info("开始保存访问记录");

        String yyyyMMddHH = new SimpleDateFormat("yyyy/MM/dd HH").format(new Date());

        Map<Object, Object> visits = redisTemplate.opsForHash().entries("visits");
        for (Object field : visits.keySet()) {
            String timeAndURL = String.valueOf(field);
            int visitsCount = Integer.parseInt(String.valueOf((visits.get(field))));
            String[] split = timeAndURL.split("\\.");
            String visitsTime = split[0];
            String pagePath = "/";

            if (split.length > 1) {
                pagePath = split[1];
            }


            if (yyyyMMddHH.equals(visitsTime)) {
                Logger.info("当时小时的访问未结束，不保存");
                continue;
            }

            QueryWrapper<PageViewStatistics> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("redis_key", timeAndURL);
            PageViewStatistics savedPageViewStatistics = pageVisitsMapper.selectOne(queryWrapper);

            if (savedPageViewStatistics != null) {
                if (visitsCount > savedPageViewStatistics.getPageView()) {
                    QueryWrapper<PageViewStatistics> updateWrapper = new QueryWrapper<>();
                    updateWrapper.eq("redis_key", savedPageViewStatistics.getRedisKey());
                    savedPageViewStatistics.setPageView(visitsCount);
                    pageVisitsMapper.update(savedPageViewStatistics, updateWrapper);
                    Logger.info("更新记录");
                }
                redisTemplate.opsForHash().delete("visits", timeAndURL);
                continue;
            }

            PageViewStatistics pageViewStatistics = new PageViewStatistics();
            pageViewStatistics.setPageView(visitsCount);

//            Log.info("redis的key："+key+"   访问路径："+path+"   访问时间："+visitsTime);


            pageViewStatistics.setViewTime(visitsTime);
            pageViewStatistics.setPagePath(pagePath);
            pageViewStatistics.setCreateTime(todayDate);
            pageViewStatistics.setRedisKey(timeAndURL);
            Logger.info(visitsTime + "访问" + pagePath + "共" + visitsCount + "次");
            pageVisitsMapper.insert(pageViewStatistics);
        }

    }


    @Override
    public List<PageViewStatisticsVo> getVisits(VisitsTimeVo visitsTimeVo) {


        LambdaQueryWrapper<PageViewStatistics> pageViewStatisticsLambdaQueryWrapper = new LambdaQueryWrapper<>();
        pageViewStatisticsLambdaQueryWrapper.ge(PageViewStatistics::getCreateTime,new Date(visitsTimeVo.getStartTime()))
                        .le(PageViewStatistics::getCreateTime,new Date(visitsTimeVo.getEndTime()))
                                .orderByAsc(PageViewStatistics::getCreateTime);

        List<PageViewStatistics> pageViewStatisticsList = pageVisitsMapper.selectList(pageViewStatisticsLambdaQueryWrapper);

        List<PageViewStatisticsVo> pageViewStatisticsVoList = new ArrayList<>();
        if(pageViewStatisticsList !=null){
            for(PageViewStatistics pageViewStatistics : pageViewStatisticsList){
                PageViewStatisticsVo pageViewStatisticsVo = new PageViewStatisticsVo();
                pageViewStatisticsVo.copy(pageViewStatistics);
                pageViewStatisticsVoList.add(pageViewStatisticsVo);
            }
        }else {
            throw new ServiceException(ResultCode.DATA_NONE);
        }

        return pageViewStatisticsVoList;


    }
}
