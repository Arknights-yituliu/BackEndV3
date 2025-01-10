package com.lhs.service.rogueSeed;

import com.lhs.entity.dto.rogueSeed.RogueSeedDTO;
import com.lhs.entity.dto.rogueSeed.RogueSeedRatingDTO;
import com.lhs.entity.vo.rogueSeed.RogueSeedPageVO;
import com.lhs.entity.vo.rogueSeed.RogueSeedRatingVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface RogueSeedService {

    /**
     * @param httpServletRequest HTTP请求对象
     * @param rogueSeedDTO  上传的种子信息
     * @return 成功信息等
     */
    Map<String, Object> saveOrUpdateRogueSeed(HttpServletRequest httpServletRequest, RogueSeedDTO rogueSeedDTO);


    void uploadRogueSeedPageToCOS();

    Map<String, Object> uploadSettlementChart(MultipartFile multipartFile, HttpServletRequest httpServletRequest);

    Map<String, Object> rogueSeedRating(RogueSeedRatingDTO rogueSeedRatingDTO, HttpServletRequest httpServletRequest);

    String getRogueSeedPageTag();

    List<RogueSeedRatingVO> listUserRougeSeedRating(HttpServletRequest httpServletRequest);

    List<RogueSeedPageVO> listRougeSeed(Integer pageSize, Integer pageNum, List<String> keywords, String order, HttpServletRequest httpServletRequest);
}
