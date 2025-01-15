package com.lhs.service.rogue;

import com.lhs.entity.dto.rogueSeed.RogueSeedDTO;
import com.lhs.entity.dto.rogueSeed.RogueSeedPageDTO;
import com.lhs.entity.dto.rogueSeed.RogueSeedRatingDTO;
import com.lhs.entity.dto.rogueSeed.RollRogueSeedDTO;
import com.lhs.entity.po.rogue.RogueSeedRating;
import com.lhs.entity.vo.rogue.RogueSeedPageVO;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface RogueSeedService {

    /**
     * @param httpServletRequest HTTP请求对象
     * @param rogueSeedDTO  上传的种子信息
     * @return 成功信息
     */
    Map<String, Object> saveOrUpdateRogueSeed(HttpServletRequest httpServletRequest, RogueSeedDTO rogueSeedDTO);

    /**
     *
     * @param multipartFile 文件对象
     * @param httpServletRequest HTTP请求对象
     * @return 成功信息
     */
    Map<String, Object> uploadSettlementChart(MultipartFile multipartFile, HttpServletRequest httpServletRequest);
    /**
     * 肉鸽种子点赞
     * rogueSeedRatingDTO 肉鸽点赞数据对象
     * @param httpServletRequest HTTP请求对象
     */
    String rogueSeedRating(RogueSeedRatingDTO rogueSeedRatingDTO, HttpServletRequest httpServletRequest);

    Map<Long, RogueSeedRating> listUserRogueSeedRating(HttpServletRequest httpServletRequest);

    List<RogueSeedPageVO> listRogueSeed(RogueSeedPageDTO rogueSeedPageDTO, HttpServletRequest httpServletRequest);

    Integer ratingStatistics();

    List<RogueSeedPageVO> rollRogueSeed(RollRogueSeedDTO rogueSeedDTO, HttpServletRequest httpServletRequest);
}
