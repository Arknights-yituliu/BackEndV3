package com.lhs.service.dev.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.entity.po.dev.DevelopLog;
import com.lhs.entity.vo.dev.DevelopLogVO;
import com.lhs.mapper.dev.DevelopLogMapper;
import com.lhs.service.dev.DevelopService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class DevelopServiceImpl implements DevelopService {

    private final DevelopLogMapper developLogMapper;


    public DevelopServiceImpl(DevelopLogMapper developLogMapper) {
        this.developLogMapper = developLogMapper;
    }

    @Override
    public void saveDevelopLog(DevelopLogVO developLogVO) {
        Date date = new Date();
        DevelopLog developLog = new DevelopLog();
        developLog.setTag(developLogVO.getTag());
        developLog.setText(developLogVO.getText());
        developLog.setAuthor(developLogVO.getAuthor());
        developLog.setCommitTime(new Date(developLogVO.getCommitTime()));
        developLog.setId(date.getTime());
        developLog.setCreateTime(date);
        developLog.setUpdateTime(date);
        developLogMapper.insert(developLog);
    }


    @Override
    public void updateDevelopLog(DevelopLog developLog) {
        developLog.setUpdateTime(new Date());
        developLogMapper.update(developLog,new QueryWrapper<DevelopLog>().eq("id",developLog.getId()));
    }
}
