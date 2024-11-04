package com.lhs.service.rogueSeed.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.TextUtil;
import com.lhs.entity.dto.rogueSeed.RogueSeedDTO;
import com.lhs.entity.dto.rogueSeed.RogueSeedPageRequest;
import com.lhs.entity.po.rogueSeed.RogueSeed;
import com.lhs.entity.po.rogueSeed.RogueSeedTag;
import com.lhs.entity.vo.rogueSeed.RogueSeedPageVO;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.rogueSeed.RogueSeedMapper;
import com.lhs.mapper.rogueSeed.RogueSeedTagMapper;
import com.lhs.service.rogueSeed.RogueSeedService;
import com.lhs.service.user.UserService;
import com.lhs.service.util.COSService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.util.*;
import java.util.stream.Collectors;

@Service
public class RogueSeedServiceImpl implements RogueSeedService {

    private final UserService userService;

    private final RedisTemplate<String, String> redisTemplate;

    private final COSService cosService;

    private final RogueSeedMapper rogueSeedMapper;
    private final RogueSeedTagMapper rogueSeedTagMapper;

    private final IdGenerator idGenerator;

    public RogueSeedServiceImpl(UserService userService, RedisTemplate<String, String> redisTemplate, COSService cosService, RogueSeedMapper rogueSeedMapper, RogueSeedTagMapper rogueSeedTagMapper) {
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.cosService = cosService;
        this.rogueSeedMapper = rogueSeedMapper;
        this.rogueSeedTagMapper = rogueSeedTagMapper;
        this.idGenerator = new IdGenerator(1L);
    }


    @Override
    public Map<String, Object> saveOrUpdateRogueSeed(RogueSeedDTO rogueSeedDTO, HttpServletRequest httpServletRequest) {

        //根据token拿到用户信息
        UserInfoVO userInfoByToken = userService.getUserInfoVOByToken(userService.extractToken(httpServletRequest));
        //获取用户uid
        Long uid = userInfoByToken.getUid();

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
        RogueSeed rogueSeed = new RogueSeed();
        rogueSeed.setUid(uid);
        createNewRogueSeedByRogueSeedDTO(rogueSeed, rogueSeedDTO);
        Integer insertBatchRow = saveRogueSeedTag(rogueSeed, rogueSeedDTO);
        int insertRow = rogueSeedMapper.insert(rogueSeed);

        //返回执行结果方便调试
        Map<String, Object> response = new HashMap<>();
        response.put("seed_affected_rows", insertRow);
        response.put("tag_affected_rows", insertBatchRow);
        return response;
    }

    @Override
    public List<RogueSeedPageVO> listRogueSeed(RogueSeedPageRequest rogueSeedPageRequest) {
        LambdaQueryWrapper<RogueSeedTag> tagLambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (rogueSeedPageRequest.getKeyWords() != null) {
            for (String keyword : rogueSeedPageRequest.getKeyWords()) {
                tagLambdaQueryWrapper.eq(RogueSeedTag::getTag,keyword);
            }
        }
        List<RogueSeedTag> rogueSeedTagList = rogueSeedTagMapper.selectList(tagLambdaQueryWrapper);
        Set<Long> seedIdSet = rogueSeedTagList.stream().map(RogueSeedTag::getSeedId).collect(Collectors.toSet());
        LambdaQueryWrapper<RogueSeed> seedLambdaQueryWrapper = new LambdaQueryWrapper<>();
        if(!seedIdSet.isEmpty()){
            seedLambdaQueryWrapper.in(RogueSeed::getSeedId, seedIdSet);
        }


        if ("rating".equals(rogueSeedPageRequest.getOrderBy())) {
            seedLambdaQueryWrapper.orderByDesc(RogueSeed::getRating);
        }

        if ("createTime".equals(rogueSeedPageRequest.getOrderBy())) {
            seedLambdaQueryWrapper.orderByDesc(RogueSeed::getCreateTime);
        }

        seedLambdaQueryWrapper.last("limit "+rogueSeedPageRequest.getPageNum()+","+rogueSeedPageRequest.getPageSize());

        List<RogueSeed> rogueSeeds = rogueSeedMapper.selectList(seedLambdaQueryWrapper);


        return createRogueSeedVOList(rogueSeeds);
    }

    @Override
    public Map<String, Object> uploadSettlementChart(MultipartFile multipartFile, HttpServletRequest httpServletRequest) {

        String imageName = idGenerator.nextId()+".jpg";
        String bucketPath = "rouge-seed/settlement-chart/"+imageName;
        cosService.uploadFile(multipartFile,bucketPath);

        Map<String, Object> response = new HashMap<>();
        response.put("imagePath",bucketPath);
        return response;
    }

    private List<RogueSeedPageVO> createRogueSeedVOList(List<RogueSeed> rogueSeedList) {
        List<RogueSeedPageVO> voList = new ArrayList<>();
        for (RogueSeed item : rogueSeedList) {
            RogueSeedPageVO rogueSeedPageVO = new RogueSeedPageVO();
            rogueSeedPageVO.setSeedId(item.getSeedId());
            rogueSeedPageVO.setSeed(item.getSeed());
            rogueSeedPageVO.setRogueVersion(item.getRogueVersion());
            rogueSeedPageVO.setDifficulty(item.getDifficulty());
            rogueSeedPageVO.setRogueTheme(item.getRogueTheme());
            rogueSeedPageVO.setRating(item.getRating());
            rogueSeedPageVO.setRatingPerson(item.getRatingPerson());
            rogueSeedPageVO.setSquad(item.getSquad());
            rogueSeedPageVO.setOperatorTeam(TextUtil.textToArray(item.getOperatorTeam()));
            rogueSeedPageVO.setDescription(item.getDescription());
            rogueSeedPageVO.setTags(TextUtil.textToArray(item.getTags()));
            rogueSeedPageVO.setSummaryImageLink(item.getSummaryImageLink());
            rogueSeedPageVO.setCreateTime(item.getCreateTime());
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
        long lastTimeStamp = rogueSeedByPO.getUpdateTime();
        //更新种子信息对象
        updateRogueSeedByRogueSeedPO(rogueSeedByPO, rogueSeedDTO);
        //更新种子信息
        int insertRow = rogueSeedMapper.updateById(rogueSeedByPO);
        //批量插入种子的tag信息
        int insertBatchRow = saveRogueSeedTag(rogueSeedByPO, rogueSeedDTO);

        //创建种子tag的查询器
        LambdaUpdateWrapper<RogueSeedTag> tagLambdaQueryWrapper = new LambdaUpdateWrapper<>();
        //根据更新时间对种子的旧tag进行逻辑删除,更新的逻辑太费劲，每天定时用脚本对标记删除的tag进行删除
        tagLambdaQueryWrapper.eq(RogueSeedTag::getCreateTime, lastTimeStamp)
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
        long currentTimeMillis = System.currentTimeMillis();
        rogueSeed.setSeed(rogueSeedDTO.getSeed());
        rogueSeed.setRogueVersion(rogueSeedDTO.getRogueVersion());
        rogueSeed.setRogueTheme(rogueSeedDTO.getRogueTheme());
        rogueSeed.setSquad(rogueSeedDTO.getSquad());
        rogueSeed.setOperatorTeam(rogueSeedDTO.getOperatorTeam());
        rogueSeed.setDifficulty(rogueSeedDTO.getDifficulty());
        rogueSeed.setDescription(rogueSeedDTO.getDescription());
        rogueSeed.setTags(String.join(",", rogueSeedDTO.getTags()));
        rogueSeed.setSummaryImageLink(rogueSeedDTO.getSummaryImageLink());
        rogueSeed.setUpdateTime(currentTimeMillis);
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
        Long currentTimeStamp = rogueSeed.getUpdateTime();
        //种子要存储的tag列表
        List<RogueSeedTag> rogueSeedTagList = new ArrayList<>();
        //将前端传来的tag转为tag对象集合
        List<String> tags = rogueSeedDTO.getTags();
        for (String tag : tags) {
            RogueSeedTag rogueSeedTag = new RogueSeedTag();
            rogueSeedTag.setTagId(idGenerator.nextId());
            rogueSeedTag.setSeedId(rogueSeedIdByPO);
            rogueSeedTag.setTag(tag);
            rogueSeedTag.setCreateTime(currentTimeStamp);
            rogueSeedTag.setDeleteFlag(false);
            rogueSeedTagList.add(rogueSeedTag);
        }
        //批量插入种子tag
        return rogueSeedTagMapper.insertBatch(rogueSeedTagList);
    }

    private void createNewRogueSeedByRogueSeedDTO(RogueSeed target, RogueSeedDTO resource) {
        long currentTimeMillis = System.currentTimeMillis();
        target.setSeedId(idGenerator.nextId());
        target.setSeed(resource.getSeed());
        target.setRating(0.0);
        target.setRatingPerson(0);
        target.setDifficulty(resource.getDifficulty());
        target.setRogueVersion(resource.getRogueVersion());
        target.setRogueTheme(resource.getRogueTheme());
        target.setSquad(resource.getSquad());
        target.setOperatorTeam(resource.getOperatorTeam());
        target.setDescription(resource.getDescription());
        target.setTags(String.join(",", resource.getTags()));
        target.setSummaryImageLink(resource.getSummaryImageLink());
        target.setCreateTime(currentTimeMillis);
        target.setUpdateTime(currentTimeMillis);
        target.setDeleteFlag(false);
    }
}
