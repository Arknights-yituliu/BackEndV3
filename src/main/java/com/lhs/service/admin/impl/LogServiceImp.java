package com.lhs.service.admin.impl;

import com.lhs.common.util.IdGenerator;
import com.lhs.entity.po.admin.LogInfo;
import com.lhs.mapper.admin.LogInfoMapper;
import com.lhs.service.admin.LogService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class LogServiceImp implements LogService {

    private final LogInfoMapper logInfoMapper;
    private final IdGenerator idGenerator;
    public LogServiceImp(LogInfoMapper logInfoMapper) {
        this.logInfoMapper = logInfoMapper;
        this.idGenerator = new IdGenerator(1L);
    }


    @Override
    public void saveLog(LogInfo loginfo) {
        idGenerator.nextId();
        loginfo.setId(idGenerator.nextId());
        loginfo.setCreateTime(new Date());
        logInfoMapper.insert(loginfo);
    }
}
