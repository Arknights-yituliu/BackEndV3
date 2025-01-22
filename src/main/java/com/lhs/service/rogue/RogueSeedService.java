package com.lhs.service.rogue;

import com.lhs.entity.dto.rogue.*;
import com.lhs.entity.po.rogue.RogueSeedRating;
import com.lhs.entity.vo.rogue.RogueSeedVO;

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

    /**
     * 获取用户评论的记录
     * @param httpServletRequest HTTP请求对象
     * @param uid uid 兜底手段 如果前端用户未登录，前端会
     * @return 用户的所有评价记录
     */
    Map<Long, RogueSeedRating> listRogueSeedUserRating(HttpServletRequest httpServletRequest,Long uid);

    /**
     * 分页获取种子
     * @param rogueSeedPageDTO  分页参数
     * @param httpServletRequest HTTP请求对象
     * @return 种子集合
     */
    List<RogueSeedVO> listRogueSeed(RogueSeedPageDTO rogueSeedPageDTO, HttpServletRequest httpServletRequest);

    /**
     * 种子评价统计
     * @return 当前种子数
     */
    Integer ratingStatistics();

    /**
     * 随机抽取一个种子
     * @param rogueSeedDTO 抽取条件
     * @param httpServletRequest  HTTP请求对象
     * @return 种子信息
     */
    RogueSeedVO rollRogueSeed(RollRogueSeedDTO rogueSeedDTO, HttpServletRequest httpServletRequest);

    /**
     * 记录用户对种子的操作
     * @param userActionOnSeedDTO 用户对种子的操作
     * @param httpServletRequest  HTTP请求对象
     */
    void recordUserActionOnSeed(UserActionOnSeedDTO userActionOnSeedDTO, HttpServletRequest httpServletRequest);
}
