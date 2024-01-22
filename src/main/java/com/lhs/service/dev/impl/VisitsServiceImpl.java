package com.lhs.service.dev.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.util.LogUtil;
import com.lhs.entity.po.dev.PageVisits;
import com.lhs.entity.vo.dev.PageVisitsVo;
import com.lhs.entity.vo.dev.VisitsTimeVo;
import com.lhs.mapper.dev.PageVisitsMapper;
import com.lhs.service.dev.VisitsService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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

    @Override
    public void savePageVisits() {
        Date todayDate = new Date();
        LogUtil.info("开始保存访问记录");

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
                LogUtil.info("当时小时的访问未结束，不保存");
                continue;
            }

            QueryWrapper<PageVisits> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("redis_key", timeAndURL);
            PageVisits savedPageVisits = pageVisitsMapper.selectOne(queryWrapper);

            if (savedPageVisits != null) {
                if (visitsCount > savedPageVisits.getVisitsCount()) {
                    QueryWrapper<PageVisits> updateWrapper = new QueryWrapper<>();
                    updateWrapper.eq("redis_key", savedPageVisits.getRedisKey());
                    savedPageVisits.setVisitsCount(visitsCount);
                    pageVisitsMapper.update(savedPageVisits, updateWrapper);
                    LogUtil.info("更新记录");
                }
                redisTemplate.opsForHash().delete("visits", timeAndURL);
                continue;
            }

            PageVisits pageVisits = new PageVisits();
            pageVisits.setVisitsCount(visitsCount);

//            Log.info("redis的key："+key+"   访问路径："+path+"   访问时间："+visitsTime);


            pageVisits.setVisitsTime(visitsTime);
            pageVisits.setPagePath(pagePath);
            pageVisits.setCreateTime(todayDate);
            pageVisits.setRedisKey(timeAndURL);
            LogUtil.info(visitsTime + "访问" + pagePath + "共" + visitsCount + "次");
            pageVisitsMapper.insert(pageVisits);
        }

    }


    @Override
    public List<PageVisitsVo> getVisits(VisitsTimeVo visitsTimeVo) {

        QueryWrapper<PageVisits> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("create_time", visitsTimeVo.getStartTime()).le("create_time", visitsTimeVo.getEndTime());

        List<PageVisits> dataList = pageVisitsMapper.selectList(queryWrapper);
        Map<String, List<PageVisits>> collectByPagePath = dataList.stream().collect(Collectors.groupingBy(PageVisits::getPagePath));
        Map<String, List<PageVisits>> collectByVisitsTime = dataList.stream().collect(Collectors.groupingBy(PageVisits::getVisitsTime));

        int sumALl = dataList.stream().mapToInt(PageVisits::getVisitsCount).sum();
        List<PageVisitsVo> pageVisitsVoList = new ArrayList<>();

        PageVisitsVo pageVisitsVoAll = new PageVisitsVo();
        pageVisitsVoAll.setPagePath("全站数据");
        pageVisitsVoAll.setVisitsCount(sumALl);

        List<PageVisits> pageVisitsListAll = new ArrayList<>();
        for (String visitsTime : collectByVisitsTime.keySet()) {
            PageVisits pageVisitsAll = new PageVisits();
            List<PageVisits> list = collectByVisitsTime.get(visitsTime);
            int sum = list.stream().mapToInt(PageVisits::getVisitsCount).sum();
            pageVisitsAll.setPagePath(list.get(0).getPagePath());
            pageVisitsAll.setVisitsTime(visitsTime);
            pageVisitsAll.setVisitsCount(sum);
            pageVisitsListAll.add(pageVisitsAll);
        }

        pageVisitsListAll.sort(Comparator.comparing(PageVisits::getVisitsTime));
        pageVisitsVoAll.setPageVisitsList(pageVisitsListAll);
        pageVisitsVoList.add(pageVisitsVoAll);


        for (String pagePath : collectByPagePath.keySet()) {
            PageVisitsVo pageVisitsVo = new PageVisitsVo();
            List<PageVisits> pageVisitsList = collectByPagePath.get(pagePath);
            pageVisitsList.sort(Comparator.comparing(PageVisits::getVisitsTime));
            int sum = pageVisitsList.stream().mapToInt(PageVisits::getVisitsCount).sum();
            pageVisitsVo.setPagePath(pagePath);
            pageVisitsVo.setVisitsCount(sum);
            pageVisitsVo.setPageVisitsList(pageVisitsList);
            pageVisitsVoList.add(pageVisitsVo);
        }

        pageVisitsVoList.sort(Comparator.comparing(PageVisitsVo::getVisitsCount).reversed());

        return pageVisitsVoList;
    }
}
