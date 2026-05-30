package com.lhs.service.material;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.entity.dto.material.PackInfoDTO;
import com.lhs.entity.po.admin.ImageInfo;
import com.lhs.entity.po.material.PackInfo;
import com.lhs.entity.vo.material.PackContentVO;
import com.lhs.entity.vo.material.PackInfoVO;
import com.lhs.entity.vo.material.PackInfoVOV5;
import com.lhs.mapper.material.PackInfoMapper;
import com.lhs.service.admin.ImageInfoService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PackInfoService {

    private final PackInfoMapper packInfoMapper;


    private final IdGenerator idGenerator;
    private final RedisTemplate<String, Object> redisTemplate;

    private final ImageInfoService imageInfoService;


    public PackInfoService(PackInfoMapper packInfoMapper,
                           RedisTemplate<String, Object> redisTemplate,



                           ImageInfoService imageInfoService) {
        this.packInfoMapper = packInfoMapper;
        this.redisTemplate = redisTemplate;

        this.idGenerator = new IdGenerator(1L);


        this.imageInfoService = imageInfoService;
    }


    @RedisCacheable(key = "Item:PackInfo", timeout = 43200)
    public List<PackInfoVOV5> listPackInfo() {
        //查询所有礼包
        LambdaQueryWrapper<PackInfo> packInfoQueryWrapper = new LambdaQueryWrapper<>();
        packInfoQueryWrapper.eq(PackInfo::getDeleteFlag, false);
        List<PackInfo> packInfoList = packInfoMapper.selectList(packInfoQueryWrapper);
        List<PackInfoVOV5> packInfoVOV5List = new ArrayList<>();
        List<ImageInfo> imageInfoList = imageInfoService.listImageInfo("");
        Map<String, String> imageLinkMap = imageInfoList.stream().collect(Collectors.toMap(ImageInfo::getImageName, ImageInfo::getImageLink));

        for (PackInfo packInfo : packInfoList) {
            PackInfoVOV5 packInfoVOV5 = new PackInfoVOV5();
            packInfoVOV5.copy(packInfo);
            String imageLink = imageLinkMap.get(packInfo.getOfficialName());
            if (imageLink != null) {
                packInfoVOV5.setImageLink(imageLink);
            }
            packInfoVOV5List.add(packInfoVOV5);
        }

        return packInfoVOV5List;
    }


    public String getPackInfoVersion() {

        Object key = redisTemplate.opsForValue().get("Item:PackInfoVersion");

        if (key == null) {
            return "2025/04/27 00:00:00";
        }

        return key.toString();

    }

    /**
     * 保存或更新礼包
     *
     * @param packInfoDTO 礼包信息
     * @return 成功消息
     */
    public String saveOrUpdatePackInfo(PackInfoDTO packInfoDTO) {
        Date currentDate = new Date();
        //创建一个po对象存储数据
        PackInfo packInfo = new PackInfo();
        //将VO类的数据传递给po
        packInfo.copy(packInfoDTO);

        if (packInfoDTO.getPackContent() != null) {
            String content = JsonMapper.toJSONString(packInfoDTO.getPackContent());
            packInfo.setContent(content);
        }

        //旧礼包需要更新,通过id查询旧礼包的信息
        LambdaQueryWrapper<PackInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PackInfo::getId, packInfoDTO.getId());
        PackInfo packInfoById = packInfoMapper.selectOne(queryWrapper);

        String message = "新增礼包成功";
        packInfo.setCreateTime(currentDate);
        //判断是新礼包还是旧礼包
        if (packInfoById == null) {
            //新礼包直接生成一个id保存到数据库
            packInfo.setId(idGenerator.nextId());
            packInfo.setDeleteFlag(false);
            packInfoMapper.insert(packInfo);

        } else {
            //如果旧礼包存在则根据id更新
            packInfoMapper.updateById(packInfo);
            message = "更新礼包成功";
        }

        redisTemplate.delete("Item:PackInfo");

        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        // 定义格式化模式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        // 格式化为字符串
        String formattedDateTime = now.format(formatter);

        redisTemplate.opsForValue().set("Item:PackInfoVersion", formattedDateTime);

        return message;
    }


    /**
     * 根据礼包id获取礼包信息
     *
     * @param idStr 礼包id
     * @return 礼包信息
     */
    public PackInfoVO getPackById(String idStr) {
        long id = Long.parseLong(idStr);
        PackInfo packInfo = packInfoMapper.selectOne(new QueryWrapper<PackInfo>().eq("id", id));

        PackInfoVO packInfoVO = getPackInfoVO(packInfo);
        ImageInfo imageInfo = imageInfoService.getImageInfo(packInfo.getOfficialName());
        if (imageInfo != null) {
            packInfoVO.setImageLink(imageInfo.getImageLink());
        }
        return packInfoVO;
    }

    /**
     * 获取全部礼包信息
     *
   
     * @return 礼包列表
     */
    public List<PackInfoVO> listAllPackInfo() {
        LambdaQueryWrapper<PackInfo> packInfoQueryWrapper = new LambdaQueryWrapper<>();
        packInfoQueryWrapper.eq(PackInfo::getDeleteFlag, false);
        List<PackInfo> packInfoList = packInfoMapper.selectList(packInfoQueryWrapper);
        List<PackInfoVO> packInfoVOList = new ArrayList<>();
        List<ImageInfo> imageInfoList = imageInfoService.listImageInfo("");
        Map<String, String> imageLinkMap = imageInfoList.stream()
                .collect(Collectors.toMap(ImageInfo::getImageName, ImageInfo::getImageLink));

        for (PackInfo packInfo : packInfoList) {
            PackInfoVO packInfoVO = getPackInfoVO(packInfo);
            String imageLink = imageLinkMap.get(packInfo.getOfficialName());
            if (imageLink != null) {
                packInfoVO.setImageLink(imageLink);
            }
            packInfoVOList.add(packInfoVO);
        }

        return packInfoVOList;
    }

    private PackInfoVO getPackInfoVO(PackInfo packInfo) {
        PackInfoVO packInfoVO = new PackInfoVO();
        packInfoVO.copy(packInfo);
        String content = packInfo.getContent();
        if (content != null && content.length() > 10) {
            List<PackContentVO> packContentVOList = JsonMapper.parseJSONArray(content, new TypeReference<List<PackContentVO>>() {
            });

            packInfoVO.setPackContent(packContentVOList);
        }

        return packInfoVO;
    }


    public String deletePackInfoById(String id) {
        PackInfo packInfo = new PackInfo();
        packInfo.setId(Long.parseLong(id));
        packInfo.setDeleteFlag(true);
        int delete = packInfoMapper.updateById(packInfo);
        return "删除了" + delete + "条礼包数据";
    }


}
