package com.lhs.service.dev;


import com.lhs.entity.po.dev.DevelopLog;
import com.lhs.entity.vo.dev.DevelopLogVO;

public interface DevelopService {

      void saveDevelopLog(DevelopLogVO developLogVO);

      void updateDevelopLog(DevelopLog developLog);
}
