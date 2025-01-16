package com.lhs.service.rogue.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.rogueSeed.*;
import com.lhs.entity.po.rogue.*;
import com.lhs.entity.vo.rogue.RogueSeedVO;

import com.lhs.entity.vo.rogue.RogueSeedRatingVO;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.rogueSeed.*;
import com.lhs.service.rogue.RogueSeedService;
import com.lhs.service.user.UserService;
import com.lhs.service.util.COSService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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


    @Override
    public Map<String, Object> saveOrUpdateRogueSeed(HttpServletRequest httpServletRequest, RogueSeedDTO rogueSeedDTO) {

        checkDTO(rogueSeedDTO);

        Long uid = 1L;

        Boolean loginStatus = userService.checkUserLoginStatus(httpServletRequest);
        if (loginStatus) {
            UserInfoVO userInfoVO = userService.getUserInfoVOByHttpServletRequest(httpServletRequest);
            uid = userInfoVO.getUid();
        }


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

        if (dto.getDescription().length() > 200) {
            throw new ServiceException(ResultCode.ROGUE_SEED_DESCRIPTION_LONGER_THAN_200_CHARACTERS);
        }

        if (dto.getSeedType() == null || dto.getSeedType() < 1 || dto.getSeedType() > 3) {
            throw new ServiceException(ResultCode.ROGUE_SEED_TYPE_IS_NULL_OR_ERROR);
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
        rogueSeed.setOperatorTeam(String.join(",", dto.getOperatorTeam()));
        rogueSeed.setDescription(dto.getDescription());
        rogueSeed.setUploadTimes(1);
        rogueSeed.setTags(String.join(",", dto.getTags()));
        rogueSeed.setSummaryImageLink(dto.getSummaryImageLink());
        rogueSeed.setRating(0.0);
        rogueSeed.setRatingCount(0);
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
        newRogueSeed.setOperatorTeam(String.join(",", dto.getOperatorTeam()));
        newRogueSeed.setDescription(dto.getDescription());
        newRogueSeed.setTags(String.join(",", dto.getTags()));
        newRogueSeed.setSummaryImageLink(dto.getSummaryImageLink());
        newRogueSeed.setUpdateTime(new Date());
        newRogueSeed.setUploadTimes(rogueSeed.getUploadTimes() + 1);
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
    public String rogueSeedRating(RogueSeedRatingDTO ratingDTO, HttpServletRequest httpServletRequest) {

        //根据token拿到用户信息
        UserInfoVO userInfoByToken = userService.getUserInfoVOByHttpServletRequest(httpServletRequest);
        //获取用户uid
        Long uid = userInfoByToken.getUid();

        //10秒内最多评论5次
//        rateLimiter.tryAcquire("Rating" + uid, 5, 10, ResultCode.TOO_MANY_RATING_ROGUE_SEED);

        LambdaUpdateWrapper<RogueSeedRating> rogueSeedRatingLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        rogueSeedRatingLambdaUpdateWrapper.eq(RogueSeedRating::getSeedId, ratingDTO.getSeedId())
                .eq(RogueSeedRating::getUid, uid);

        //根据唯一id查出上次点赞记录
        RogueSeedRating ratingPo = rogueSeedRatingMapper.selectOne(rogueSeedRatingLambdaUpdateWrapper);


        //查出的点赞记录如果不存在则进行新增点赞记录
        if (ratingPo == null) {
            return createNewRogueSeedRating(ratingDTO, uid);
        } else {
            return updateRogueSeedRating(ratingPo, ratingDTO);
        }

    }

    private String createNewRogueSeedRating(RogueSeedRatingDTO ratingDTO, Long uid) {
        Date date = new Date();
        RogueSeedRating rogueSeedRating = new RogueSeedRating();

        rogueSeedRating.setRatingId(idGenerator.nextId());
        rogueSeedRating.setRating(ratingDTO.getRating());
        rogueSeedRating.setUid(uid);
        rogueSeedRating.setSeedId(ratingDTO.getSeedId());
        rogueSeedRating.setCreateTime(date);
        rogueSeedRating.setUpdateTime(date);
        rogueSeedRating.setDeleteFlag(false);
        rogueSeedRatingMapper.insert(rogueSeedRating);

        redisTemplate.opsForZSet().incrementScore("RogueSeedThumbsUp", ratingDTO.getSeedId(), ratingDTO.getRating());
        redisTemplate.opsForZSet().incrementScore("RogueSeedRatingCount", ratingDTO.getSeedId(), 1);

        return "评价成功";
    }

    /**
     * 更新点赞信息
     *
     * @param po  用户在数据库中的点赞信息
     * @param dto 用户从前端传来的数据
     */
    private String updateRogueSeedRating(RogueSeedRating po, RogueSeedRatingDTO dto) {


        Date date = new Date();
        //创建一个评论行为的数据库对象
        RogueSeedRating rogueSeedRating = new RogueSeedRating();
        rogueSeedRating.setRatingId(po.getRatingId());
        rogueSeedRating.setRating(dto.getRating());
        rogueSeedRating.setUpdateTime(date);

//        System.out.println(po.getRating() + "===" + dto.getRating());

        //如果数据库中的删除标记是true，代表之前用户取消了评价后再次进行了评价
        if (po.getDeleteFlag()) {
            //将删除标记改为false
            rogueSeedRating.setDeleteFlag(false);
            //更新到数据库
            rogueSeedRatingMapper.updateById(rogueSeedRating);
            //将redis中的该种子的点赞人次+1
            redisTemplate.opsForZSet().incrementScore("RogueSeedRatingCount", dto.getSeedId(), 1);
            //如果该次评价是点赞，则把redis中的点赞总数+1
            if (dto.getRating() == 1) {
                redisTemplate.opsForZSet().incrementScore("RogueSeedThumbsUp", dto.getSeedId(), 1);
            }
            return "评价成功";
        }

        //两次评价相同证明用户想取消评价
        if (Objects.equals(po.getRating(), dto.getRating())) {
            //将数据库中的评价记录的删除标记改为true
            rogueSeedRating.setDeleteFlag(true);
            //更新到数据库
            rogueSeedRatingMapper.updateById(rogueSeedRating);
            //取消评价的话，评价总人数减少-1
            redisTemplate.opsForZSet().incrementScore("RogueSeedRatingCount", dto.getSeedId(), -1);
            //如果取消的是点赞评价，点赞总数也要-1
            if (dto.getRating() == 1) {
                redisTemplate.opsForZSet().incrementScore("RogueSeedThumbsUp", dto.getSeedId(), -1);
            }
            return "取消评价";
        }


        //如果不是以上两种特殊情况则直接更新到数据库
        rogueSeedRatingMapper.updateById(rogueSeedRating);

        //用户更新评价
        if (dto.getRating() == 1) {
            //更新的评价是点赞，点赞总数+1
            redisTemplate.opsForZSet().incrementScore("RogueSeedThumbsUp", dto.getSeedId(), 1);
        }
        if (dto.getRating() == 1) {
            //更新的评价是点踩，点赞总数-1
            redisTemplate.opsForZSet().incrementScore("RogueSeedThumbsUp", dto.getSeedId(), -1);
        }

        return "评价成功";
    }

    @Override
    public Integer ratingStatistics() {
        //查询所有未删除的评价记录
        LambdaUpdateWrapper<RogueSeedRating> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(RogueSeedRating::getDeleteFlag, false);
        List<RogueSeedRatingVO> rogueSeedRatingList = rogueSeedRatingMapper.listRogueSeedRating(0, 50000);
        int size = rogueSeedRatingList.size();
        LogUtils.info("本次统计的种子评价数量为：" + size);
        //将旧的评价统计记录删除
        LambdaUpdateWrapper<RogueSeedRatingStatistics> statisticsLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        statisticsLambdaUpdateWrapper.set(RogueSeedRatingStatistics::getDeleteFlag, true).eq(RogueSeedRatingStatistics::getDeleteFlag, false);
        rogueSeedRatingStatisticsMapper.update(null, statisticsLambdaUpdateWrapper);

        //查询种子的数据库id和类型，方便向每个种子的统计记录中写入种子类型，方便roll种子时直接用统计记录roll，不需要查询种子表
        List<RogueSeedIdAndTypeDTO> rogueSeedList = rogueSeedMapper.listRogueSeedIdAndType();
        Map<Long, Integer> seedIdAndType = rogueSeedList.stream().collect(Collectors.toMap(RogueSeedIdAndTypeDTO::getSeedId, RogueSeedIdAndTypeDTO::getSeedType));


        Map<Long, RogueSeedRatingStatistics> collect = rogueSeedRatingList.stream().collect(Collectors.groupingBy(
                RogueSeedRatingVO::getSeedId, // 按照seedId进行分组
                Collectors.collectingAndThen(
                        Collectors.toList(), // 将相同seedId的数据收集到列表中
                        list -> {
                            int ratingCount = list.size(); // rating数量
                            long likeCount = list.stream().filter(e -> e.getRating() == 1).count();// 计算总评分
                            RogueSeedRatingStatistics result = new RogueSeedRatingStatistics();
                            result.setRating((double) likeCount); // 存储评分数量
                            result.setRatingCount(ratingCount); // 存储平均评分
                            result.setCreateTime(new Date());
                            result.setDeleteFlag(false);
                            return result;
                        }
                )
        ));


        collect.forEach((k, v) -> {
            v.setSeedId(k);
            Integer type = seedIdAndType.get(k);
            v.setSeedType(type != null ? type : -1);
            rogueSeedRatingStatisticsMapper.insert(v);
            redisTemplate.opsForZSet().add("RogueSeedThumbsUp", k, v.getRating());
            redisTemplate.opsForZSet().add("RogueSeedRatingCount", k, v.getRatingCount());
        });

        LogUtils.info("种子点赞统计已更新");

        return size;
    }


    @Override
    public Map<Long, RogueSeedRating> listRogueSeedUserRating(HttpServletRequest httpServletRequest) {
        Map<Long, RogueSeedRating> collect = new HashMap<>();
        Boolean loginStatus = userService.checkUserLoginStatus(httpServletRequest);
        if (!loginStatus) {
            return collect;
        }
        //根据token拿到用户信息
        UserInfoVO userInfoByToken = userService.getUserInfoVOByHttpServletRequest(httpServletRequest);
        //获取用户uid
        Long uid = userInfoByToken.getUid();
        LambdaUpdateWrapper<RogueSeedRating> rogueSeedRatingLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        rogueSeedRatingLambdaUpdateWrapper.eq(RogueSeedRating::getUid, uid).eq(RogueSeedRating::getDeleteFlag,false);
        List<RogueSeedRating> rogueSeedRatings = rogueSeedRatingMapper.selectList(rogueSeedRatingLambdaUpdateWrapper);
        collect = rogueSeedRatings.stream().collect(Collectors.toMap(RogueSeedRating::getSeedId, Function.identity()));

        return collect;
    }


    @Override
    public List<RogueSeedVO> listRogueSeed(RogueSeedPageDTO rogueSeedPageDTO, HttpServletRequest httpServletRequest) {

        LambdaQueryWrapper<RogueSeed> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RogueSeed::getSeedType, rogueSeedPageDTO.getSeedType()).last("limit " + rogueSeedPageDTO.getPageNum() + "," + rogueSeedPageDTO.getPageSize());

        if ("date".equals(rogueSeedPageDTO.getSortCondition())) {
            queryWrapper.orderByDesc(RogueSeed::getCreateTime);
        } else {
            queryWrapper.orderByDesc(RogueSeed::getRating);
        }



        List<RogueSeed> rogueSeedList = rogueSeedMapper.selectList(queryWrapper);

        return createRogueSeedVOList(rogueSeedList);
    }


    @Override
    public RogueSeedVO rollRogueSeed(RollRogueSeedDTO rollDTO, HttpServletRequest httpServletRequest) {
        Long seedCount = getRogueSeedCountByType(rollDTO.getSeedType());
        List<RogueSeedRatingStatistics> ratingStatisticsList = new ArrayList<>();

        if (seedCount < 300) {
            ratingStatisticsList = rogueSeedRatingStatisticsMapper.pageRogueSeedRatingStatistics(0, seedCount.intValue());
        } else {
            ratingStatisticsList = rogueSeedRatingStatisticsMapper.pageRogueSeedRatingStatistics(0, (int) (seedCount / 5));
        }

        Random random = new Random();
        int index = random.nextInt(ratingStatisticsList.size()); // 生成一个[0, array.length)的随机数

        Long seedId = ratingStatisticsList.get(index).getSeedId();
        RogueSeed rogueSeed = rogueSeedMapper.selectById(seedId);

        return rogueSeedPOWriteInDTO(rogueSeed);
    }

    @RedisCacheable(key = "RogueSeedCountByType", timeout = 1200, paramOrMethod = "param")
    private Long getRogueSeedCountByType(Integer seedType) {
        LambdaQueryWrapper<RogueSeedRatingStatistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RogueSeedRatingStatistics::getSeedType, seedType).eq(RogueSeedRatingStatistics::getDeleteFlag, false);
        Long count = rogueSeedRatingStatisticsMapper.selectCount(queryWrapper);
        if (count < 1) {
            count = Long.valueOf(ratingStatistics());
        }

        if (count < 10) {
            throw new ServiceException(ResultCode.ROGUE_SEED_COUNT_LESS_THAN_10);
        }

        return count;
    }


    private List<RogueSeedVO> createRogueSeedVOList(List<RogueSeed> rogueSeedList) {
        List<RogueSeedVO> voList = new ArrayList<>();

        for (RogueSeed item : rogueSeedList) {
            RogueSeedVO rogueSeedVO = rogueSeedPOWriteInDTO(item);
            voList.add(rogueSeedVO);
        }
        return voList;
    }


    private RogueSeedVO rogueSeedPOWriteInDTO(RogueSeed po) {
        RogueSeedVO rogueSeedVO = new RogueSeedVO();
        rogueSeedVO.setSeedId(po.getSeedId());
        rogueSeedVO.setSeed(po.getSeed());
        rogueSeedVO.setRogueVersion(po.getRogueVersion());
        rogueSeedVO.setDifficulty(po.getDifficulty());
        rogueSeedVO.setRogueTheme(po.getRogueTheme());
        rogueSeedVO.setSquad(po.getSquad());
        rogueSeedVO.setOperatorTeam(TextUtil.textToArray(po.getOperatorTeam()));
        rogueSeedVO.setDescription(po.getDescription());
        rogueSeedVO.setTags(TextUtil.textToArray(po.getTags()));
        rogueSeedVO.setSummaryImageLink(po.getSummaryImageLink());
        rogueSeedVO.setCreateTime(po.getCreateTime().getTime());
        rogueSeedVO.setUploadTimes(po.getUploadTimes());
        Double rogueSeedThumbsUp = redisTemplate.opsForZSet().score("RogueSeedThumbsUp", po.getSeedId());
        Double rogueSeedRatingCount = redisTemplate.opsForZSet().score("RogueSeedRatingCount", po.getSeedId());
        if (rogueSeedThumbsUp != null && rogueSeedRatingCount != null) {
            rogueSeedVO.setRating(rogueSeedThumbsUp / rogueSeedRatingCount * 5);
            rogueSeedVO.setRatingCount(rogueSeedRatingCount.intValue());
        } else {
            rogueSeedVO.setRating(0.0);
            rogueSeedVO.setRatingCount(0);
        }

        return rogueSeedVO;
    }


}
