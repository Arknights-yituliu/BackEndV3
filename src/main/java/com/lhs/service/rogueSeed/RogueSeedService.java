package com.lhs.service.rogueSeed;

import com.lhs.entity.dto.rogueSeed.RogueSeedDTO;
import com.lhs.entity.dto.rogueSeed.RogueSeedPageRequest;
import com.lhs.entity.vo.rogueSeed.RogueSeedPageVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface RogueSeedService {

    Map<String,Object> saveOrUpdateRogueSeed(RogueSeedDTO rogueSeedDTO, HttpServletRequest httpServletRequest);

    List<RogueSeedPageVO> listRogueSeed(RogueSeedPageRequest rogueSeedDTO);

    Map<String, Object> uploadSettlementChart(MultipartFile multipartFile, HttpServletRequest httpServletRequest);

    Map<String, Object> rogueSeedRating(Double rating, HttpServletRequest httpServletRequest);
}
