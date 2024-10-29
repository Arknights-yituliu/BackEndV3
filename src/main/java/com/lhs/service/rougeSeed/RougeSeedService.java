package com.lhs.service.rougeSeed;

import com.lhs.entity.dto.rougeSeed.RougeSeedDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface RougeSeedService {

    Map<String,Object> saveOrUpdateRougeSeed(RougeSeedDTO rougeSeedDTO, HttpServletRequest httpServletRequest);

}
