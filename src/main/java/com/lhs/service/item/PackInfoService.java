package com.lhs.service.item;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.po.admin.ImageInfo;
import com.lhs.entity.po.item.PackContent;
import com.lhs.entity.po.item.PackInfo;
import com.lhs.entity.po.item.PackItem;
import com.lhs.entity.vo.item.PackContentVO;
import com.lhs.entity.vo.item.PackInfoVO;
import com.lhs.mapper.item.PackContentMapper;
import com.lhs.mapper.item.PackInfoMapper;
import com.lhs.mapper.item.PackItemMapper;
import com.lhs.mapper.item.service.PackContentMapperService;
import com.lhs.service.admin.ImageInfoService;
import com.lhs.service.util.COSService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PackInfoService {

    private final PackInfoMapper packInfoMapper;
    private final PackContentMapper packContentMapper;
    private final PackItemMapper packItemMapper;
    private final IdGenerator idGenerator;
    private final RedisTemplate<String, Object> redisTemplate;
    private final COSService cosService;
    private final PackContentMapperService packContentMapperService;
    private final ImageInfoService imageInfoService;

    public PackInfoService(PackInfoMapper packInfoMapper, PackContentMapper packContentMapper,
                           RedisTemplate<String, Object> redisTemplate, COSService cosService,
                           PackContentMapperService packContentMapperService,
                           PackItemMapper packItemMapper,ImageInfoService imageInfoService) {
        this.packInfoMapper = packInfoMapper;
        this.packContentMapper = packContentMapper;
        this.idGenerator = new IdGenerator(1L);
        this.redisTemplate = redisTemplate;
        this.cosService = cosService;
        this.packContentMapperService = packContentMapperService;
        this.packItemMapper = packItemMapper;
        this.imageInfoService = imageInfoService;
    }


    @RedisCacheable(key = "Item:PackData")
    public List<PackInfoVO> listPackInfo() {
        return listAllPackInfo();
    }


    public List<PackInfoVO> listAllPackInfo() {
        //查询所有礼包
        List<PackInfo> packInfoList = packInfoMapper.selectList(null);

        //根据上面内容id的集合对礼包内容进行查询
        List<PackContent> packContentList = packContentMapper.selectList(null);

        //查询出来的礼包内容根据packId进行一个分组
        Map<Long, List<PackContent>> mapPackContentByContentId = packContentList.stream()
                .collect(Collectors.groupingBy(PackContent::getContentId));

        //将礼包价值表转为map对象，方便使用
        Map<String, Double> itemValueMap = packItemMapper.selectList(null)
                .stream().collect(Collectors.toMap(PackItem::getId, PackItem::getValue));

        List<PackInfoVO> VOList = new ArrayList<>();

        List<ImageInfo> imageInfoList = imageInfoService.listImageInfo("");
        Map<String, String> imageLinkMap = imageInfoList.stream().collect(Collectors.toMap(ImageInfo::getImageName, ImageInfo::getImageLink));

        for (PackInfo packInfo : packInfoList) {
            PackInfoVO packInfoVO = getPackInfoVO(packInfo, mapPackContentByContentId.get(packInfo.getContentId()));
            packInfoVO.setImageLink(imageLinkMap.get(packInfo.getOfficialName()));
            VOList.add(packInfoVO);
            packPromotionRatioCalc(packInfoVO, itemValueMap);
        }
        return VOList;
    }


    public String saveOrUpdatePackInfo(PackInfoVO packInfoVO) {
        Date currentDate = new Date();
        //创建一个po对象存储数据
        PackInfo packInfo = new PackInfo();
        //将VO类的数据传递给po
        packInfo.copy(packInfoVO);
        Long contentId = idGenerator.nextId();
        packInfo.setContentId(contentId);

        //旧礼包需要更新,通过id查询旧礼包的信息
        LambdaQueryWrapper<PackInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PackInfo::getId, packInfoVO.getId());
        PackInfo packInfoById = packInfoMapper.selectOne(queryWrapper);

        String message = "新增礼包成功";

        //判断是新礼包还是旧礼包
        if (packInfoById == null) {
            //新礼包直接生成一个id保存到数据库
            packInfo.setId(idGenerator.nextId());
            packInfo.setCreateTime(new Date());
            packInfo.setDeleteFlag(false);
            packInfoMapper.insert(packInfo);
            packInfo.setCreateTime(currentDate);
        } else {
            //如果旧礼包存在则根据id更新
            packInfoMapper.updateById(packInfo);
            message = "更新礼包成功";
        }

        //礼包id
        Long packId = packInfo.getId();

        //礼包没有除四种抽卡资源之外内容直接返回礼包信息
        if (packInfoVO.getPackContent() == null) {
            return message;
        }

        //礼包的额外内容
        List<PackContentVO> packContentVOList = packInfoVO.getPackContent();
        //创建一个礼包额外内容的po类的集合
        List<PackContent> packContentList = new ArrayList<>();
        //将vo类的内容拷贝到po,同时生成礼包id
        for (PackContentVO packContentVO : packContentVOList) {
            PackContent packContent = new PackContent();
            packContent.copy(packContentVO);
            packContent.setId(idGenerator.nextId());
            packContent.setContentId(contentId);
            packContent.setPackId(packId);
            packContentList.add(packContent);
        }


        //批量保存
        packContentMapperService.saveBatch(packContentList);

        redisTemplate.delete("Item:PackData");

        return message;
    }


    public PackInfoVO getPackById(String idStr) {
        long id = Long.parseLong(idStr);
        PackInfo packInfo = packInfoMapper.selectOne(new QueryWrapper<PackInfo>().eq("id", id));
        LambdaQueryWrapper<PackContent> packContentQueryWrapper = new LambdaQueryWrapper<>();
        packContentQueryWrapper.eq(PackContent::getContentId, packInfo.getContentId());
        List<PackContent> packContentList = packContentMapper.selectList(packContentQueryWrapper);
        return getPackInfoVO(packInfo, packContentList);
    }


    public PackItem saveOrUpdatePackItem(PackItem newPackItem) {
        QueryWrapper<PackItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", newPackItem.getId());
        PackItem packItem = packItemMapper.selectOne(queryWrapper);
        if (packItem == null) {
            newPackItem.setId(String.valueOf(idGenerator.nextId()));
            packItemMapper.insert(newPackItem);
        } else {
            packItemMapper.update(newPackItem, queryWrapper);
        }
        return newPackItem;
    }


    public List<PackItem> listPackItem() {
        return packItemMapper.selectList(null);

    }


    public void deletePackItemById(String id) {
        QueryWrapper<PackItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        packItemMapper.delete(queryWrapper);
    }


    public String clearPackInfoCache() {
        Boolean delete = redisTemplate.delete("Item:PackData");
        if (Boolean.FALSE.equals(delete)) {
            throw new ServiceException(ResultCode.REDIS_CLEAR_CACHE_ERROR);
        }
        return "缓存清除成功";
    }


    private PackInfoVO getPackInfoVO(PackInfo packInfo, List<PackContent> packContentList) {
        PackInfoVO packInfoVO = new PackInfoVO();
        packInfoVO.copy(packInfo);
        if (packContentList != null) {
            List<PackContentVO> packContentVOList = new ArrayList<>();
            for (PackContent packContent : packContentList) {
                PackContentVO packContentVO = new PackContentVO();
                packContentVO.copy(packContent);
                packContentVOList.add(packContentVO);
                packInfoVO.setPackContent(packContentVOList);
            }
        }

        return packInfoVO;
    }

    private void packPromotionRatioCalc(PackInfoVO packInfoVO, Map<String, Double> itemValue) {
        double drawPrice = 0.0; //每一抽价格
        double packedOriginiumPrice = 0.0; //每源石（折算物资后）价格
        double drawEfficiency = 0.0; //氪金性价比
        double packEfficiency = 0.0; //综合性价比
        double packedOriginium = 0.0;
        double draws = 0.0;

        double totalOfOrundum = packInfoVO.getOrundum() + packInfoVO.getOriginium() * 180
                + packInfoVO.getGachaTicket() * 600 + packInfoVO.getTenGachaTicket() * 6000;

        double eachOriginalOriginiumPrice = 648 / 185.0;
        double eachOriginalDrawPrice = 648.0 / 185 / 0.3;

        if (totalOfOrundum > 0) {
            //计算共计多少抽
            draws = totalOfOrundum / 600;
            //计算等效多少源石 1源石 = 180合成玉
            packedOriginium += totalOfOrundum / 180;
            //计算每一抽的价格
            drawPrice = packInfoVO.getPrice() / draws;
            //计算抽卡性价比
            drawEfficiency = eachOriginalDrawPrice / drawPrice;
            //计算每个源石的价格
            packedOriginiumPrice = packInfoVO.getPrice() / packedOriginium;
            //计算综合性价比
            packEfficiency = eachOriginalOriginiumPrice / packedOriginiumPrice;
        }

        List<PackContentVO> packContentVOList = packInfoVO.getPackContent();
        //当这个礼包的物品不为空时
        if (packContentVOList != null) {
            double apCount = 0.0;
            for (PackContentVO packContentVO : packContentVOList) {
                if (itemValue.get(packContentVO.getItemId()) != null) {
                    apCount += itemValue.get(packContentVO.getItemId()) * packContentVO.getQuantity();
                }
            }
            packedOriginium += apCount / 135;
            if (packedOriginium > 0) {
                packedOriginiumPrice = packInfoVO.getPrice() / packedOriginium;
                packEfficiency = eachOriginalOriginiumPrice / packedOriginiumPrice;
            }
        }

        packInfoVO.setDraws(draws);
        packInfoVO.setDrawPrice(drawPrice);
        packInfoVO.setPackedOriginiumPrice(packedOriginiumPrice);
        packInfoVO.setDrawEfficiency(drawEfficiency);
        packInfoVO.setPackedOriginium(packedOriginium);
        packInfoVO.setPackEfficiency(packEfficiency);
    }
}
