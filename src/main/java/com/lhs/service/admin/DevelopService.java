package com.lhs.service.admin;


import com.lhs.entity.po.admin.DevelopLog;
import com.lhs.entity.vo.dev.DevelopLogVO;

public interface DevelopService {

      void saveDevelopLog(DevelopLogVO developLogVO);

      void updateDevelopLog(DevelopLog developLog);
}
