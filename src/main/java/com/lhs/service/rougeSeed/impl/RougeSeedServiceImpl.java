package com.lhs.service.rougeSeed.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lhs.common.util.IdGenerator;
import com.lhs.entity.dto.rougeSeed.RougeSeedDTO;
import com.lhs.entity.po.rougeSeed.RougeSeed;
import com.lhs.entity.po.rougeSeed.RougeSeedTag;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.rougeSeed.RougeSeedMapper;
import com.lhs.mapper.rougeSeed.RougeSeedTagMapper;
import com.lhs.service.rougeSeed.RougeSeedService;
import com.lhs.service.user.UserService;
import com.lhs.service.util.COSService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RougeSeedServiceImpl implements RougeSeedService {

    private final UserService userService;

    private final RedisTemplate<String, String> redisTemplate;

    private final COSService cosService;

    private final RougeSeedMapper rougeSeedMapper;
    private final RougeSeedTagMapper rougeSeedTagMapper;

    private final IdGenerator idGenerator;

    public RougeSeedServiceImpl(UserService userService, RedisTemplate<String, String> redisTemplate, COSService cosService, RougeSeedMapper rougeSeedMapper, RougeSeedTagMapper rougeSeedTagMapper) {
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.cosService = cosService;
        this.rougeSeedMapper = rougeSeedMapper;
        this.rougeSeedTagMapper = rougeSeedTagMapper;
        this.idGenerator = new IdGenerator(1L);
    }



    @Override
    public Map<String, Object> saveOrUpdateRougeSeed(RougeSeedDTO rougeSeedDTO, HttpServletRequest httpServletRequest) {

        //根据token拿到用户信息
        UserInfoVO userInfoByToken = userService.getUserInfoVOByToken(userService.extractToken(httpServletRequest));
        //获取用户uid
        Long uid = userInfoByToken.getUid();

        //判断前端传来的数据对象是否有种子id
        if(rougeSeedDTO.getSeedId()!=null){
            //判断前端传来的种子id是否存在了数据库中
            RougeSeed rougeSeedByPO = rougeSeedMapper.selectById(rougeSeedDTO.getSeedId());
            //如果存在则对数据库中的种子更新
            if(rougeSeedByPO!=null){
                return updateRougeSeed(rougeSeedByPO,rougeSeedDTO);
            }
        }

        //创建一个新种子对象
        RougeSeed rougeSeed = new RougeSeed();
        rougeSeed.setUid(uid);
        createNewRougeSeedByRougeSeedDTO(rougeSeed,rougeSeedDTO);
        Integer insertBatchRow = saveRougeSeedTag(rougeSeed, rougeSeedDTO);
        int insertRow = rougeSeedMapper.insert(rougeSeed);

        //返回执行结果方便调试
        Map<String, Object> response = new HashMap<>();
        response.put("seed_affected_rows",insertRow);
        response.put("tag_affected_rows",insertBatchRow);
        return response;
    }

    /**
     * 更新种子信息
     * @param rougeSeedByPO 数据库中的种子对象
     * @param rougeSeedDTO 前端传来的数据对象
     * @return
     */
    private Map<String,Object> updateRougeSeed(RougeSeed rougeSeedByPO, RougeSeedDTO rougeSeedDTO){
        //获取种子的更新时间，根据种子的上次更新时间去将旧tag删除
        long lastTimeStamp = rougeSeedByPO.getUpdateTime();
        //更新种子信息对象
        updateRougeSeedByRougeSeedPO(rougeSeedByPO,rougeSeedDTO);
        //更新种子信息
        int insertRow = rougeSeedMapper.updateById(rougeSeedByPO);
        //批量插入种子的tag信息
        int insertBatchRow = saveRougeSeedTag(rougeSeedByPO,rougeSeedDTO);

        //创建种子tag的查询器
        LambdaUpdateWrapper<RougeSeedTag> tagLambdaQueryWrapper = new LambdaUpdateWrapper<>();
        //根据更新时间对种子的旧tag进行逻辑删除,更新的逻辑太费劲，每天定时用脚本对标记删除的tag进行删除
        tagLambdaQueryWrapper.eq(RougeSeedTag::getCreateTime,lastTimeStamp)
                .set(RougeSeedTag::getDeleteFlag,true);
        //最后执行tag的逻辑删除
        int deleteRow = rougeSeedTagMapper.update(null,tagLambdaQueryWrapper);

        //返回执行结果方便调试
        Map<String, Object> response = new HashMap<>();
        response.put("seed_affected_rows",insertRow);
        response.put("tag_affected_rows",insertBatchRow);
        response.put("delete_tag_affected_rows",deleteRow);
        return response;
    }

    /**
     * 将前端传来的数据对象赋给数据库中的种子对象
     * @param rougeSeed 数据库中的种子对象
     * @param rougeSeedDTO 前端传来的数据对象
     */
    private void updateRougeSeedByRougeSeedPO(RougeSeed rougeSeed, RougeSeedDTO rougeSeedDTO){
        long currentTimeMillis = System.currentTimeMillis();
        rougeSeed.setSeed(rougeSeedDTO.getSeed());
        rougeSeed.setRougeVersion(rougeSeedDTO.getRougeVersion());
        rougeSeed.setRougeTheme(rougeSeedDTO.getRougeTheme());
        rougeSeed.setSquad(rougeSeedDTO.getSquad());
        rougeSeed.setOperatorTeam(rougeSeedDTO.getOperatorTeam());
        rougeSeed.setDescription(rougeSeedDTO.getDescription());
        rougeSeed.setTags(String.join(",",rougeSeedDTO.getTags()));
        rougeSeed.setSummaryImageLink(rougeSeedDTO.getSummaryImageLink());
        rougeSeed.setUpdateTime(currentTimeMillis);
        rougeSeed.setDeleteFlag(false);
    }

    /**
     * 保存种子tag信息
     * @param rougeSeed 种子对象
     * @param rougeSeedDTO 前端传来的数据对象
     * @return 保存的tag条数
     */
    private Integer saveRougeSeedTag(RougeSeed rougeSeed,RougeSeedDTO rougeSeedDTO){
        //获取种子id
        Long rougeSeedIdByPO = rougeSeed.getSeedId();
        //获取种子更新时间
        Long currentTimeStamp = rougeSeed.getUpdateTime();
        //种子要存储的tag列表
        List<RougeSeedTag> rougeSeedTagList = new ArrayList<>();
        //将前端传来的tag转为tag对象集合
        List<String> tags = rougeSeedDTO.getTags();
        for(String tag:tags){
            RougeSeedTag rougeSeedTag = new RougeSeedTag();
            rougeSeedTag.setTagId(idGenerator.nextId());
            rougeSeedTag.setSeedId(rougeSeedIdByPO);
            rougeSeedTag.setTag(tag);
            rougeSeedTag.setCreateTime(currentTimeStamp);
            rougeSeedTagList.add(rougeSeedTag);
        }
        //批量插入种子tag
        return rougeSeedTagMapper.insertBatch(rougeSeedTagList);
    }

    private void createNewRougeSeedByRougeSeedDTO(RougeSeed target,RougeSeedDTO resource){
        long currentTimeMillis = System.currentTimeMillis();
        target.setSeedId(idGenerator.nextId());
        target.setSeed(resource.getSeed());
        target.setRating(0);
        target.setRougeVersion(resource.getRougeVersion());
        target.setRougeTheme(resource.getRougeTheme());
        target.setSquad(resource.getSquad());
        target.setOperatorTeam(resource.getOperatorTeam());
        target.setDescription(resource.getDescription());
        target.setTags(String.join(",",resource.getTags()));
        target.setSummaryImageLink(resource.getSummaryImageLink());
        target.setCreateTime(currentTimeMillis);
        target.setUpdateTime(currentTimeMillis);
        target.setDeleteFlag(false);
    }
}
