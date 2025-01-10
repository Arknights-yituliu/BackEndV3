package com.lhs.service.rogueSeed.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.rogueSeed.RogueSeedDTO;
import com.lhs.entity.dto.rogueSeed.RogueSeedRatingDTO;
import com.lhs.entity.po.rogueSeed.RogueSeed;
import com.lhs.entity.po.rogueSeed.RogueSeedRating;
import com.lhs.entity.po.rogueSeed.RogueSeedRatingStatistics;
import com.lhs.entity.po.rogueSeed.RogueSeedTag;
import com.lhs.entity.vo.rogueSeed.RogueSeedPageVO;
import com.lhs.entity.vo.rogueSeed.RogueSeedRatingVO;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.rogueSeed.RogueSeedMapper;
import com.lhs.mapper.rogueSeed.RogueSeedRatingMapper;
import com.lhs.mapper.rogueSeed.RogueSeedRatingStatisticsMapper;
import com.lhs.mapper.rogueSeed.RogueSeedTagMapper;
import com.lhs.service.rogueSeed.RogueSeedService;
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
    private final RogueSeedTagMapper rogueSeedTagMapper;
    private final RogueSeedRatingMapper rogueSeedRatingMapper;
    private final RateLimiter rateLimiter;
    private final IdGenerator idGenerator;
    private final RogueSeedRatingStatisticsMapper rogueSeedRatingStatisticsMapper;

    public RogueSeedServiceImpl(UserService userService,
                                RedisTemplate<String, Object> redisTemplate,
                                COSService cosService,
                                RogueSeedMapper rogueSeedMapper,
                                RogueSeedTagMapper rogueSeedTagMapper, RogueSeedRatingMapper rogueSeedRatingMapper, RateLimiter rateLimiter, RogueSeedRatingStatisticsMapper rogueSeedRatingStatisticsMapper) {
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.cosService = cosService;
        this.rogueSeedMapper = rogueSeedMapper;
        this.rogueSeedTagMapper = rogueSeedTagMapper;
        this.rogueSeedRatingMapper = rogueSeedRatingMapper;
        this.rateLimiter = rateLimiter;
        this.rogueSeedRatingStatisticsMapper = rogueSeedRatingStatisticsMapper;
        this.idGenerator = new IdGenerator(1L);
    }


    @Override
    public Map<String, Object> saveOrUpdateRogueSeed(HttpServletRequest httpServletRequest, RogueSeedDTO rogueSeedDTO) {

        checkDTO(rogueSeedDTO);

        //根据token拿到用户信息
        UserInfoVO userInfoByToken = userService.getUserInfoVOByHttpServletRequest(httpServletRequest);
        //获取用户uid
        Long uid = userInfoByToken.getUid();

        LambdaUpdateWrapper<RogueSeed> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(RogueSeed::getSeed, rogueSeedDTO.getSeed());

        //判断前端传来的数据对象是否有种子id
        if (rogueSeedDTO.getSeedId() != null) {
            //判断前端传来的种子id是否存在了数据库中
            RogueSeed rogueSeedByPO = rogueSeedMapper.selectById(rogueSeedDTO.getSeedId());
            //如果存在则对数据库中的种子更新
            if (rogueSeedByPO != null) {
                return updateRogueSeed(rogueSeedByPO, rogueSeedDTO);
            }
        }

        //创建一个新种子对象
        RogueSeed rogueSeed = rogueSeedCopyByDTO(rogueSeedDTO);

        Integer insertBatchRow = saveRogueSeedTag(rogueSeed, rogueSeedDTO);
        int insertRow = rogueSeedMapper.insert(rogueSeed);

        //返回执行结果方便调试
        Map<String, Object> response = new HashMap<>();
        response.put("seed_affected_rows", insertRow);
        response.put("tag_affected_rows", insertBatchRow);
        return response;
    }

    private void checkDTO(RogueSeedDTO dto) {
        if (dto.getSeed() == null || dto.getSeed().length() < 20) {
            throw new ServiceException(ResultCode.ROGUE_SEED_IS_NULL_OR_FORMAT_ERROR);
        }
        if (dto.getRogueTheme() == null || dto.getRogueVersion() == null || dto.getSource() == null) {
            throw new ServiceException(ResultCode.ROGUE_SEED_PARAMS_IS_NULL);
        }
    }


    private RogueSeed rogueSeedCopyByDTO(RogueSeedDTO dto) {
        RogueSeed rogueSeed = new RogueSeed();
        Date date = new Date();
        rogueSeed.setSeedId(idGenerator.nextId());
        rogueSeed.setSeed(dto.getSeed());
        rogueSeed.setRatingCount(0);
        rogueSeed.setDifficulty(dto.getDifficulty());
        rogueSeed.setRogueVersion(dto.getRogueVersion());
        rogueSeed.setRogueTheme(dto.getRogueTheme());
        rogueSeed.setSquad(dto.getSquad());
        rogueSeed.setOperatorTeam(dto.getOperatorTeam());
        rogueSeed.setDescription(dto.getDescription());
        rogueSeed.setTags(String.join(",", dto.getTags()));
        rogueSeed.setSummaryImageLink(dto.getSummaryImageLink());
        rogueSeed.setCreateTime(date);
        rogueSeed.setUpdateTime(date);
        rogueSeed.setDeleteFlag(false);

        return rogueSeed;
    }

    @Override
    public void uploadRogueSeedPageToCOS() {
        long currentTimeMillis = System.currentTimeMillis();
        List<RogueSeed> rogueSeedList = rogueSeedMapper.selectList(null);
        List<RogueSeedRatingStatistics> rogueSeedRatingStatistics = rogueSeedRatingStatisticsMapper.selectList(null);
        Map<Long, RogueSeedRatingStatistics> collect = rogueSeedRatingStatistics.stream()
                .collect(Collectors.toMap(RogueSeedRatingStatistics::getSeedId, Function.identity()));
        Map<String, Object> response = new HashMap<>();
        List<RogueSeedPageVO> rogueSeedVOList = createRogueSeedVOList(rogueSeedList, collect);
        response.put("list", rogueSeedVOList);
        response.put("updateTime", currentTimeMillis);
        Logger.info("本次肉鸽种子上传数量为" + rogueSeedVOList.size() + "条");
        cosService.uploadJson(JsonMapper.toJSONString(response), "/rogue-seed/page/" + currentTimeMillis + ".json");
        redisTemplate.opsForValue().set("RougeSeedPageTag", currentTimeMillis);
    }

    @Override
    public String getRogueSeedPageTag() {
        Object tag = redisTemplate.opsForValue().get("RougeSeedPageTag");
        return tag != null ? tag.toString() : "NO_DATA";
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

        rateLimiter.tryAcquire("Rating" + uid, 15, 30, ResultCode.TOO_MANY_RATING_ROGUE_SEED);

        LambdaUpdateWrapper<RogueSeedRating> rogueSeedRatingLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        rogueSeedRatingLambdaUpdateWrapper.eq(RogueSeedRating::getSeedId, rogueSeedRatingDTO.getSeedId())
                .eq(RogueSeedRating::getUid, uid);

        //根据唯一id查出上次点赞记录
        RogueSeedRating rogueSeedRatingByDB = rogueSeedRatingMapper.selectOne(rogueSeedRatingLambdaUpdateWrapper);

        //查出的点赞记录如果不存在则进行新增点赞记录
        if (rogueSeedRatingByDB == null) {
            return createNewRogueSeedRating(rogueSeedRatingDTO, uid);
        }

        Map<String, Object> response = new HashMap<>();
        RogueSeedRating rogueSeedRating = new RogueSeedRating();
        rogueSeedRating.setUpdateTime(new Date());
        rogueSeedRating.setRatingId(rogueSeedRatingByDB.getRatingId());
        rogueSeedRating.setRating(rogueSeedRatingDTO.getRating());
        rogueSeedRatingMapper.updateById(rogueSeedRating);
        response.put("ratingId", rogueSeedRatingDTO.getRatingId());
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

        List<RogueSeed> rogueSeedList = rogueSeedMapper.pageRogueSeedOrderByConditionAnd(order, pageNum, pageSize);

        return null;
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


    private Map<String, Object> createNewRogueSeedRating(RogueSeedRatingDTO rogueSeedRatingDTO, Long uid) {
        Date date = new Date();
        RogueSeedRating rogueSeedRating = new RogueSeedRating();
        Long ratingId = idGenerator.nextId();
        rogueSeedRating.setRatingId(ratingId);
        rogueSeedRating.setRating(rogueSeedRatingDTO.getRating());
        rogueSeedRating.setUid(uid);
        rogueSeedRating.setSeedId(rogueSeedRatingDTO.getSeedId());
        rogueSeedRating.setCreateTime(date);
        rogueSeedRating.setUpdateTime(date);
        rogueSeedRatingMapper.insert(rogueSeedRating);
        Map<String, Object> response = new HashMap<>();
        response.put("ratingId", ratingId);
        return response;
    }


    private List<RogueSeedPageVO> createRogueSeedVOList(List<RogueSeed> rogueSeedList, Map<Long, RogueSeedRatingStatistics> collect) {
        List<RogueSeedPageVO> voList = new ArrayList<>();
        for (RogueSeed item : rogueSeedList) {
            RogueSeedPageVO rogueSeedPageVO = new RogueSeedPageVO();
            rogueSeedPageVO.setSeedId(item.getSeedId());
            rogueSeedPageVO.setSeed(item.getSeed());
            rogueSeedPageVO.setRogueVersion(item.getRogueVersion());
            rogueSeedPageVO.setDifficulty(item.getDifficulty());
            rogueSeedPageVO.setRogueTheme(item.getRogueTheme());
            rogueSeedPageVO.setRatingPerson(item.getRatingCount());
            rogueSeedPageVO.setSquad(item.getSquad());
            rogueSeedPageVO.setOperatorTeam(TextUtil.textToArray(item.getOperatorTeam()));
            rogueSeedPageVO.setDescription(item.getDescription());
            rogueSeedPageVO.setTags(TextUtil.textToArray(item.getTags()));
            rogueSeedPageVO.setSummaryImageLink(item.getSummaryImageLink());
            rogueSeedPageVO.setCreateTime(item.getCreateTime().getTime());
            RogueSeedRatingStatistics rogueSeedRatingStatistics = collect.get(item.getSeedId());
            if (rogueSeedRatingStatistics != null) {
                rogueSeedPageVO.setRating(rogueSeedRatingStatistics);
            } else {
                rogueSeedPageVO.setRating(new RogueSeedRatingStatistics());
            }
            voList.add(rogueSeedPageVO);
        }
        return voList;
    }


    /**
     * 更新种子信息
     *
     * @param rogueSeedByPO 数据库中的种子对象
     * @param rogueSeedDTO  前端传来的数据对象
     * @return
     */
    private Map<String, Object> updateRogueSeed(RogueSeed rogueSeedByPO, RogueSeedDTO rogueSeedDTO) {
        //获取种子的更新时间，根据种子的上次更新时间去将旧tag删除
        Date updateTime = rogueSeedByPO.getUpdateTime();
        //更新种子信息对象
        updateRogueSeedByRogueSeedPO(rogueSeedByPO, rogueSeedDTO);
        //更新种子信息
        int insertRow = rogueSeedMapper.updateById(rogueSeedByPO);
        //批量插入种子的tag信息
        int insertBatchRow = saveRogueSeedTag(rogueSeedByPO, rogueSeedDTO);

        //创建种子tag的查询器
        LambdaUpdateWrapper<RogueSeedTag> tagLambdaQueryWrapper = new LambdaUpdateWrapper<>();
        //根据更新时间对种子的旧tag进行逻辑删除,更新的逻辑太费劲，每天定时用脚本对标记删除的tag进行删除
        tagLambdaQueryWrapper.eq(RogueSeedTag::getCreateTime, updateTime)
                .eq(RogueSeedTag::getSeedId, rogueSeedByPO.getSeedId())
                .set(RogueSeedTag::getDeleteFlag, true);
        //最后执行tag的逻辑删除
        int deleteRow = rogueSeedTagMapper.update(null, tagLambdaQueryWrapper);

        //返回执行结果方便调试
        Map<String, Object> response = new HashMap<>();
        response.put("seed_affected_rows", insertRow);
        response.put("tag_affected_rows", insertBatchRow);
        response.put("delete_tag_affected_rows", deleteRow);
        return response;
    }

    /**
     * 将前端传来的数据对象赋给数据库中的种子对象
     *
     * @param rogueSeed    数据库中的种子对象
     * @param rogueSeedDTO 前端传来的数据对象
     */
    private void updateRogueSeedByRogueSeedPO(RogueSeed rogueSeed, RogueSeedDTO rogueSeedDTO) {
        rogueSeed.setSeed(rogueSeedDTO.getSeed());
        rogueSeed.setRogueVersion(rogueSeedDTO.getRogueVersion());
        rogueSeed.setRogueTheme(rogueSeedDTO.getRogueTheme());
        rogueSeed.setSquad(rogueSeedDTO.getSquad());
        rogueSeed.setOperatorTeam(rogueSeedDTO.getOperatorTeam());
        rogueSeed.setDifficulty(rogueSeedDTO.getDifficulty());
        rogueSeed.setDescription(rogueSeedDTO.getDescription());
        rogueSeed.setTags(String.join(",", rogueSeedDTO.getTags()));
        rogueSeed.setSummaryImageLink(rogueSeedDTO.getSummaryImageLink());
        rogueSeed.setUpdateTime(new Date());
        rogueSeed.setDeleteFlag(false);
    }

    /**
     * 保存种子tag信息
     *
     * @param rogueSeed    种子对象
     * @param rogueSeedDTO 前端传来的数据对象
     * @return 保存的tag条数
     */
    private Integer saveRogueSeedTag(RogueSeed rogueSeed, RogueSeedDTO rogueSeedDTO) {
        //获取种子id
        Long rogueSeedIdByPO = rogueSeed.getSeedId();
        //获取种子更新时间
        Date updateTime = rogueSeed.getUpdateTime();
        //种子要存储的tag列表
        List<RogueSeedTag> rogueSeedTagList = new ArrayList<>();
        //将前端传来的tag转为tag对象集合
        List<String> tags = rogueSeedDTO.getTags();
        for (String tag : tags) {
            RogueSeedTag rogueSeedTag = new RogueSeedTag();
            rogueSeedTag.setTagId(idGenerator.nextId());
            rogueSeedTag.setSeedId(rogueSeedIdByPO);
            rogueSeedTag.setTag(tag);
            rogueSeedTag.setCreateTime(updateTime);
            rogueSeedTag.setDeleteFlag(false);
            rogueSeedTagList.add(rogueSeedTag);
        }
        //批量插入种子tag
        return rogueSeedTagMapper.insertBatch(rogueSeedTagList);
    }


}
