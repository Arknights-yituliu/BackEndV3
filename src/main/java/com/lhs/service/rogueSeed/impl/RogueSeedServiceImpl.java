package com.lhs.service.rogueSeed.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.rogueSeed.RogueSeedDTO;
import com.lhs.entity.dto.rogueSeed.RogueSeedRatingDTO;
import com.lhs.entity.po.rogueSeed.*;
import com.lhs.entity.vo.rogueSeed.RogueSeedPageVO;
import com.lhs.entity.vo.rogueSeed.RogueSeedRatingVO;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.rogueSeed.*;
import com.lhs.service.rogueSeed.RogueSeedService;
import com.lhs.service.user.UserService;
import com.lhs.service.util.COSService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.util.*;

@Service
public class RogueSeedServiceImpl implements RogueSeedService {

    private final UserService userService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final COSService cosService;

    private final RogueSeedMapper rogueSeedMapper;
    private final RogueSeedBakMapper rogueSeedBakMapper;
    private final RogueSeedTagMapper rogueSeedTagMapper;
    private final RogueSeedRatingMapper rogueSeedRatingMapper;
    private final RateLimiter rateLimiter;
    private final IdGenerator idGenerator;
    private final RogueSeedRatingStatisticsMapper rogueSeedRatingStatisticsMapper;

    public RogueSeedServiceImpl(UserService userService,
                                RedisTemplate<String, Object> redisTemplate,
                                COSService cosService,
                                RogueSeedMapper rogueSeedMapper,
                                RogueSeedBakMapper rogueSeedBakMapper,
                                RogueSeedTagMapper rogueSeedTagMapper,
                                RogueSeedRatingMapper rogueSeedRatingMapper,
                                RateLimiter rateLimiter,
                                RogueSeedRatingStatisticsMapper rogueSeedRatingStatisticsMapper) {
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.cosService = cosService;
        this.rogueSeedMapper = rogueSeedMapper;
        this.rogueSeedBakMapper = rogueSeedBakMapper;
        this.rogueSeedTagMapper = rogueSeedTagMapper;
        this.rogueSeedRatingMapper = rogueSeedRatingMapper;
        this.rateLimiter = rateLimiter;
        this.rogueSeedRatingStatisticsMapper = rogueSeedRatingStatisticsMapper;
        this.idGenerator = new IdGenerator(1L);
    }

    public static final Map<String, String> ROGUE_SEED_TYPE = Map.of(
            "胡种", "1",
            "毒种", "2",
            "特殊种", "3"
    );

    @Override
    public Map<String, Object> saveOrUpdateRogueSeed(HttpServletRequest httpServletRequest, RogueSeedDTO rogueSeedDTO) {

        checkDTO(rogueSeedDTO);

        //根据token拿到用户信息
        UserInfoVO userInfoByToken = userService.getUserInfoVOByHttpServletRequest(httpServletRequest);
        //获取用户uid
        Long uid = userInfoByToken.getUid();

        //搜索条件为种子
        LambdaUpdateWrapper<RogueSeed> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(RogueSeed::getSeed, rogueSeedDTO.getSeed());
        //根据条件搜索种子
        RogueSeed rogueSeedPO = rogueSeedMapper.selectOne(lambdaUpdateWrapper);

        //未记录该种子
        if (rogueSeedPO == null) {
            return insertRogueSeed(rogueSeedDTO, uid);
        }

        //已经记录该种子
        return updateRogueSeed(rogueSeedPO, rogueSeedDTO, uid);

    }

    private void checkDTO(RogueSeedDTO dto) {
        if (dto.getSeed() == null || dto.getSeed().length() < 20) {
            throw new ServiceException(ResultCode.ROGUE_SEED_IS_NULL_OR_FORMAT_ERROR);
        }
        if (dto.getRogueTheme() == null || dto.getRogueVersion() == null || dto.getSource() == null || dto.getDescription() == null) {

            throw new ServiceException(ResultCode.ROGUE_SEED_PARAMS_IS_NULL);
        }

        if (ROGUE_SEED_TYPE.get(dto.getSeedType()) == null) {
            throw new ServiceException(ResultCode.ROGUE_SEED_PARAMS_IS_NULL);
        }
    }


    /**
     * 新增一个种子
     *
     * @param dto 前端数据对象
     * @return 数据库对象
     */
    private Map<String, Object> insertRogueSeed(RogueSeedDTO dto, Long uid) {
        RogueSeed rogueSeed = new RogueSeed();
        rogueSeed.setSeedId(idGenerator.nextId());
        rogueSeed.setSeed(dto.getSeed());
        rogueSeed.setSeedType(dto.getSeedType());
        rogueSeed.setDifficulty(dto.getDifficulty());
        rogueSeed.setSource(dto.getSource());
        rogueSeed.setScore(dto.getScore());
        rogueSeed.setRogueVersion(dto.getRogueVersion());
        rogueSeed.setRogueTheme(dto.getRogueTheme());
        rogueSeed.setSquad(String.join(",", dto.getSquad()));
        rogueSeed.setOperatorTeam(String.join(",",dto.getOperatorTeam()));
        rogueSeed.setDescription(dto.getDescription());
        rogueSeed.setRatingCount(0);
        rogueSeed.setUploadTimes(1);
        rogueSeed.setTags(String.join(",", dto.getTags()));
        rogueSeed.setSummaryImageLink(dto.getSummaryImageLink());

        Date date = new Date();
        rogueSeed.setCreateTime(date);
        rogueSeed.setUpdateTime(date);
        rogueSeed.setDeleteFlag(false);

        int insertRow = rogueSeedMapper.insert(rogueSeed);
        backupSeedDescription(rogueSeed, uid);
        //返回执行结果方便调试
        Map<String, Object> response = new HashMap<>();
        response.put("seed_affected_rows", insertRow);
        return response;

    }

    private Map<String, Object> updateRogueSeed(RogueSeed rogueSeed, RogueSeedDTO dto, Long uid) {
        RogueSeed newRogueSeed = new RogueSeed();
        newRogueSeed.setSeedId(rogueSeed.getSeedId());
        newRogueSeed.setSeed(dto.getSeed());
        newRogueSeed.setSeedType(dto.getSeedType());
        newRogueSeed.setDifficulty(dto.getDifficulty());
        newRogueSeed.setSource(dto.getSource());
        newRogueSeed.setScore(dto.getScore());
        newRogueSeed.setRogueVersion(dto.getRogueVersion());
        newRogueSeed.setRogueTheme(dto.getRogueTheme());
        newRogueSeed.setSquad(String.join(",", dto.getSquad()));
        newRogueSeed.setOperatorTeam(String.join(",",dto.getOperatorTeam()));
        newRogueSeed.setDescription(dto.getDescription());
        newRogueSeed.setTags(String.join(",", dto.getTags()));
        newRogueSeed.setSummaryImageLink(dto.getSummaryImageLink());
        newRogueSeed.setUpdateTime(new Date());
        newRogueSeed.setUploadTimes(rogueSeed.getUploadTimes()+1);
        newRogueSeed.setCreateTime(rogueSeed.getCreateTime());
        backupSeedDescription(newRogueSeed, uid);
        int updateRow = rogueSeedMapper.updateById(newRogueSeed);

        //返回执行结果方便调试
        Map<String, Object> response = new HashMap<>();
        response.put("seed_affected_rows", updateRow);
        return response;
    }

    private void backupSeedDescription(RogueSeed rogueSeed, Long uid) {
        RogueSeedBak rogueSeedBak = new RogueSeedBak();
        rogueSeedBak.setSeedId(idGenerator.nextId());
        rogueSeedBak.setSeed(rogueSeed.getSeed());
        rogueSeedBak.setUid(uid);
        rogueSeedBak.setDescription(rogueSeed.getDescription());
        rogueSeedBak.setCreateTime(rogueSeed.getCreateTime());
        rogueSeedBakMapper.insert(rogueSeedBak);
    }


    @Override
    public Map<String, Object> uploadSettlementChart(MultipartFile multipartFile, HttpServletRequest httpServletRequest) {

        String imageName = idGenerator.nextId() + ".jpg";
        String bucketPath = "rogue-seed/settlement-chart/" + imageName;
        cosService.uploadFile(multipartFile, bucketPath);

        Map<String, Object> response = new HashMap<>();
        response.put("imagePath", bucketPath);
        return response;
    }

    @Override
    public Map<String, Object> rogueSeedRating(RogueSeedRatingDTO rogueSeedRatingDTO, HttpServletRequest httpServletRequest) {

        //根据token拿到用户信息
        UserInfoVO userInfoByToken = userService.getUserInfoVOByHttpServletRequest(httpServletRequest);
        //获取用户uid
        Long uid = userInfoByToken.getUid();

        //10秒内最多评论5次
        rateLimiter.tryAcquire("Rating" + uid, 5, 10, ResultCode.TOO_MANY_RATING_ROGUE_SEED);

        LambdaUpdateWrapper<RogueSeedRating> rogueSeedRatingLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        rogueSeedRatingLambdaUpdateWrapper.eq(RogueSeedRating::getSeedId, rogueSeedRatingDTO.getSeedId())
                .eq(RogueSeedRating::getUid, uid);

        //根据唯一id查出上次点赞记录
        RogueSeedRating rogueSeedRatingByDB = rogueSeedRatingMapper.selectOne(rogueSeedRatingLambdaUpdateWrapper);

        //查出的点赞记录如果不存在则进行新增点赞记录
        if (rogueSeedRatingByDB == null) {
            return createNewRogueSeedRating(rogueSeedRatingDTO, uid);
        }

        return updateRogueSeedRating(rogueSeedRatingByDB, rogueSeedRatingDTO, uid);

    }

    private Map<String, Object> createNewRogueSeedRating(RogueSeedRatingDTO rogueSeedRatingDTO, Long uid) {
        Date date = new Date();
        RogueSeedRating rogueSeedRating = new RogueSeedRating();

        rogueSeedRating.setRatingId(idGenerator.nextId());
        rogueSeedRating.setRating(rogueSeedRatingDTO.getRating());
        rogueSeedRating.setUid(uid);
        rogueSeedRating.setSeedId(rogueSeedRatingDTO.getSeedId());
        rogueSeedRating.setCreateTime(date);
        rogueSeedRating.setUpdateTime(date);
        rogueSeedRatingMapper.insert(rogueSeedRating);
        Map<String, Object> response = new HashMap<>();
        return response;
    }

    private Map<String, Object> updateRogueSeedRating(RogueSeedRating rogueSeedRatingByDB, RogueSeedRatingDTO rogueSeedRatingDTO, Long uid) {
        Date date = new Date();
        RogueSeedRating rogueSeedRating = new RogueSeedRating();
        rogueSeedRating.setRatingId(rogueSeedRatingByDB.getRatingId());
        rogueSeedRating.setRating(rogueSeedRatingDTO.getRating());
        rogueSeedRating.setUpdateTime(date);
        rogueSeedRatingMapper.updateById(rogueSeedRating);
        Map<String, Object> response = new HashMap<>();
        return response;
    }

    @Override
    public List<RogueSeedRatingVO> listUserRougeSeedRating(HttpServletRequest httpServletRequest) {

        //根据token拿到用户信息
        UserInfoVO userInfoByToken = userService.getUserInfoVOByHttpServletRequest(httpServletRequest);
        //获取用户uid
        Long uid = userInfoByToken.getUid();
        LambdaUpdateWrapper<RogueSeedRating> rogueSeedRatingLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        rogueSeedRatingLambdaUpdateWrapper.eq(RogueSeedRating::getUid, uid);
        List<RogueSeedRating> rogueSeedRatings = rogueSeedRatingMapper.selectList(rogueSeedRatingLambdaUpdateWrapper);
        return createRougeSeedRatingList(rogueSeedRatings);
    }


    @Override
    public List<RogueSeedPageVO> listRougeSeed(Integer pageSize, Integer pageNum, List<String> keywords, String order, HttpServletRequest httpServletRequest) {

        List<RogueSeed> rogueSeedList = rogueSeedMapper.pageRogueSeedOrderByCondition(order, pageNum, pageSize);


        return createRogueSeedVOList(rogueSeedList);
    }

    public List<RogueSeedRatingVO> createRougeSeedRatingList(List<RogueSeedRating> list) {
        List<RogueSeedRatingVO> voList = new ArrayList<>();
        for (RogueSeedRating item : list) {
            RogueSeedRatingVO rogueSeedRatingVO = new RogueSeedRatingVO();
            rogueSeedRatingVO.setRatingId(item.getRatingId());
            rogueSeedRatingVO.setSeedId(item.getSeedId());
            rogueSeedRatingVO.setRating(item.getRating());
            rogueSeedRatingVO.setCreateTime(item.getCreateTime());
            voList.add(rogueSeedRatingVO);
        }

        return voList;
    }


    private List<RogueSeedPageVO> createRogueSeedVOList(List<RogueSeed> rogueSeedList) {
        List<RogueSeedPageVO> voList = new ArrayList<>();
        Object o = redisTemplate.opsForValue().get("RogueSeedStatistics");
        Map<Long, Integer> rogueSeedStatisticsCollect = new HashMap<>();
        if (o != null) {
            rogueSeedStatisticsCollect = JsonMapper.parseObject(String.valueOf(o), new TypeReference<>() {
            });
        }

        for (RogueSeed item : rogueSeedList) {
            RogueSeedPageVO rogueSeedPageVO = new RogueSeedPageVO();
            rogueSeedPageVO.setSeedId(item.getSeedId());
            rogueSeedPageVO.setSeed(item.getSeed());
            rogueSeedPageVO.setRogueVersion(item.getRogueVersion());
            rogueSeedPageVO.setDifficulty(item.getDifficulty());
            rogueSeedPageVO.setRogueTheme(item.getRogueTheme());
            rogueSeedPageVO.setRatingCount(item.getRatingCount());
            rogueSeedPageVO.setSquad(item.getSquad());
            rogueSeedPageVO.setOperatorTeam(TextUtil.textToArray(item.getOperatorTeam()));
            rogueSeedPageVO.setDescription(item.getDescription());
            rogueSeedPageVO.setTags(TextUtil.textToArray(item.getTags()));
            rogueSeedPageVO.setSummaryImageLink(item.getSummaryImageLink());
            rogueSeedPageVO.setCreateTime(item.getCreateTime().getTime());
            Integer rating = rogueSeedStatisticsCollect.get(item.getSeedId());
            rogueSeedPageVO.setRating(rating==null?-1:rating);
            voList.add(rogueSeedPageVO);
        }
        return voList;
    }


}
