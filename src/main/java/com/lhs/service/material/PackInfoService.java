package com.lhs.service.material;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.enums.ResultCode;
import com.lhs.entity.dto.material.PackInfoDTO;
import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.po.admin.ImageInfo;
import com.lhs.entity.po.material.Item;
import com.lhs.entity.po.material.PackContent;
import com.lhs.entity.po.material.PackInfo;
import com.lhs.entity.po.material.ItemCustom;
import com.lhs.entity.vo.material.PackContentVO;
import com.lhs.entity.vo.material.PackInfoVO;
import com.lhs.mapper.material.PackContentMapper;
import com.lhs.mapper.material.PackInfoMapper;
import com.lhs.mapper.material.ItemCustomMapper;
import com.lhs.mapper.material.service.PackContentMapperService;
import com.lhs.service.admin.ImageInfoService;
import com.lhs.service.util.TencentCloudService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PackInfoService {

    private final PackInfoMapper packInfoMapper;
    private final PackContentMapper packContentMapper;
    private final ItemCustomMapper itemCustomMapper;
    private final IdGenerator idGenerator;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TencentCloudService tencentCloudService;
    private final PackContentMapperService packContentMapperService;
    private final ItemService itemService;
    private final ImageInfoService imageInfoService;



    public PackInfoService(PackInfoMapper packInfoMapper,
                           PackContentMapper packContentMapper,
                           RedisTemplate<String, Object> redisTemplate,
                           TencentCloudService tencentCloudService,
                           PackContentMapperService packContentMapperService,
                           ItemCustomMapper itemCustomMapper,
                           ItemService itemService,
                           ImageInfoService imageInfoService) {
        this.packInfoMapper = packInfoMapper;
        this.packContentMapper = packContentMapper;
        this.redisTemplate = redisTemplate;
        this.itemService = itemService;
        this.idGenerator = new IdGenerator(1L);
        this.tencentCloudService = tencentCloudService;
        this.packContentMapperService = packContentMapperService;
        this.itemCustomMapper = itemCustomMapper;
        this.imageInfoService = imageInfoService;
    }




    public List<PackInfoVO> listPackInfo(StageConfigDTO stageConfigDTO) {
        return listAllPackInfo(stageConfigDTO);
    }


    public List<PackInfoVO> listAllPackInfo(StageConfigDTO stageConfigDTO) {
        //查询所有礼包
        LambdaQueryWrapper<PackInfo> packInfoQueryWrapper = new LambdaQueryWrapper<>();
        packInfoQueryWrapper.eq(PackInfo::getDeleteFlag, false);
        List<PackInfo> packInfoList = packInfoMapper.selectList(packInfoQueryWrapper);

        //根据上面内容id的集合对礼包内容进行查询
        List<PackContent> packContentList = packContentMapper.selectList(null);

        //查询出来的礼包内容根据packId进行一个分组
        Map<Long, List<PackContent>> mapPackContentByContentId = packContentList.stream().collect(Collectors.groupingBy(PackContent::getContentId));

        //将礼包价值表转为map对象，方便使用
        Map<String, Double> itemValueMap = listCustomItem(stageConfigDTO)
                .stream()
                .collect(Collectors.toMap(ItemCustom::getItemId, ItemCustom::getItemValue));

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


    public String saveOrUpdatePackInfo( PackInfoDTO packInfoDTO) {
        Date currentDate = new Date();
        //创建一个po对象存储数据
        PackInfo packInfo = new PackInfo();
        //将VO类的数据传递给po
        packInfo.copy(packInfoDTO);
        Long contentId = idGenerator.nextId();
        packInfo.setContentId(contentId);

        //旧礼包需要更新,通过id查询旧礼包的信息
        LambdaQueryWrapper<PackInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PackInfo::getId, packInfoDTO.getId());
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
            Long oldContentId = packInfoById.getContentId();
            int deleteRows = packContentMapper.deleteById(oldContentId);
            //如果旧礼包存在则根据id更新
            packInfoMapper.updateById(packInfo);
            message = "更新礼包成功，删除了" + deleteRows + "条内容数据";
        }

        //礼包id
        Long packId = packInfo.getId();

        //礼包没有除四种抽卡资源之外内容直接返回礼包信息
        if (packInfoDTO.getPackContent() == null) {
            return message;
        }

        //礼包的额外内容
        List<PackContentVO> packContentVOList = packInfoDTO.getPackContent();
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

        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        uploadPackInfoPageToCos(stageConfigDTO);

        return message;
    }


    public PackInfoVO getPackById(String idStr) {
        long id = Long.parseLong(idStr);
        PackInfo packInfo = packInfoMapper.selectOne(new QueryWrapper<PackInfo>().eq("id", id));
        LambdaQueryWrapper<PackContent> packContentQueryWrapper = new LambdaQueryWrapper<>();
        packContentQueryWrapper.eq(PackContent::getContentId, packInfo.getContentId());
        List<PackContent> packContentList = packContentMapper.selectList(packContentQueryWrapper);
        PackInfoVO packInfoVO = getPackInfoVO(packInfo, packContentList);
        ImageInfo imageInfo = imageInfoService.getImageInfo(packInfo.getOfficialName());
        if(imageInfo!=null){
            packInfoVO.setImageLink(imageInfo.getImageLink());
        }
        return packInfoVO;
    }


    public ItemCustom saveOrUpdatePackItem(ItemCustom newItemCustom) {
        LambdaQueryWrapper<ItemCustom> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ItemCustom::getItemId, newItemCustom.getItemId());
        ItemCustom itemCustom = itemCustomMapper.selectOne(queryWrapper);
        if (itemCustom == null) {
            newItemCustom.setItemId(String.valueOf(idGenerator.nextId()));
            itemCustomMapper.insert(newItemCustom);
        } else {
            itemCustomMapper.update(newItemCustom, queryWrapper);
        }
        return newItemCustom;
    }


    public List<ItemCustom> listCustomItem(StageConfigDTO stageConfigDTO) {

        List<Item> itemList = itemService.getItemListCache(stageConfigDTO);
        List<ItemCustom> itemCustomList = new ArrayList<>();

        for(Item item:itemList){
            ItemCustom itemCustom = new ItemCustom();
            itemCustom.setItemId(item.getItemId());
            itemCustom.setItemValue(item.getItemValueAp());
            itemCustom.setItemName(item.getItemName());
            itemCustom.setZoneIndex(item.getCardNum());
            itemCustomList.add(itemCustom);
        }

        List<ItemCustom> itemCustoms = itemCustomMapper.selectList(null);
        itemCustomList.addAll(itemCustoms);

        return itemCustomList;

    }


    public String deletePackItemById(String id) {
        QueryWrapper<ItemCustom> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        int delete = itemCustomMapper.delete(queryWrapper);
        return "删除了" + delete + "条物品数据";
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

        //源石性价比基准
        double eachOriginalOriginiumPrice = 648 / 185.0;
        //抽卡性价比基准
        double eachOriginalDrawPrice = 648.0 / 185 / 0.3;

        //System.out.println(packInfoVO.getOfficialName());

        double draws = 0.0;//抽数
        double drawPrice = 0.0; //每一抽价格
        double packedOriginiumPrice = 0.0; //每源石（折算物资后）价格
        double drawEfficiency = 0.0; //仅抽卡性价比
        double packEfficiency = 0.0; //综合性价比
        double packedOriginium = 0.0; //礼包总价值折合成源石


        double drawsKernel = 0.0;//抽数（含蓝抽）
        double drawPriceKernel = 0.0; //每一抽价格（含蓝抽）
        double packedOriginiumPriceKernel = 0.0; //每源石（折算物资后）价格（含蓝抽）
        double drawEfficiencyKernel = 0.0; //仅抽卡性价比（含蓝抽）
        double packEfficiencyKernel = 0.0; //综合性价比（含蓝抽）
        double packedOriginiumKernel = 0.0; //礼包总价值折合成源石（含蓝抽）

        double apCount = 0.0;//总价值（理智）
        double apCountKernel = 0.0;//总价值（理智，含蓝抽）

        //礼包内的物品的集合
        List<PackContentVO> packContentVOList = packInfoVO.getPackContent();
        //直接计算抽数
        draws = (double) packInfoVO.getOrundum() / 600 + packInfoVO.getOriginium() * 0.3 + packInfoVO.getGachaTicket() + packInfoVO.getTenGachaTicket() * 10;
        apCount += draws * 450;
        drawsKernel += draws;
        apCountKernel += apCount;
        //System.out.println("黄抽抽数 {} "+draws);
        //礼包物资转为理智
        if (packContentVOList != null) {
            for (PackContentVO packContentVO : packContentVOList) {
                //判断是否有不存在物品表中的物品
                if (itemValue.get(packContentVO.getItemId()) != null) {
                    //蓝抽单独计算
                    if (packContentVO.getItemId().equals("classic_gacha")) {
                        drawsKernel += packContentVO.getQuantity();
                    } else if (packContentVO.getItemId().equals("classic_gacha_10")) {
                        drawsKernel += packContentVO.getQuantity() * 10;
                    } else {
                        apCount += itemValue.get(packContentVO.getItemId()) * packContentVO.getQuantity();
                    }
                    //System.out.println(packContentVO.getItemName()+" {} "+itemValue.get(packContentVO.getItemId())+" * "+packContentVO.getQuantity());
                    apCountKernel += itemValue.get(packContentVO.getItemId()) * packContentVO.getQuantity();
                    //两不耽误，各算各的
                }
            }
        }

        //总价值计算
        packedOriginium = apCount / 135;//总源石
        packedOriginiumKernel += apCountKernel / 135;//总源石（含蓝抽）

        //每源石花费计算

        packedOriginiumPrice = packedOriginium > 0 ? packInfoVO.getPrice() / packedOriginium : 0;


        packedOriginiumPriceKernel = packedOriginiumKernel > 0 ? packInfoVO.getPrice() / packedOriginiumKernel : 0;
        //综合性价比计算
        packEfficiency = packedOriginiumPrice > 0 ? eachOriginalOriginiumPrice / packedOriginiumPrice : 0;
        packEfficiencyKernel = packedOriginiumPriceKernel > 0 ? eachOriginalOriginiumPrice / packedOriginiumPriceKernel : 0;

        //抽卡性价比计算
        //计算每一抽的价格
        drawPrice = draws > 0 ? packInfoVO.getPrice() / draws : 0;
        //计算抽卡性价比
        drawEfficiency = drawPrice > 0 ? eachOriginalDrawPrice / drawPrice : 0;

        //抽卡性价比计算(含蓝抽)
        //计算每一抽的价格
        drawPriceKernel = drawsKernel > 0 ? packInfoVO.getPrice() / drawsKernel : 0;
        //计算抽卡性价比
        drawEfficiencyKernel = drawPriceKernel > 0 ? eachOriginalDrawPrice / drawPriceKernel : 0;

        packInfoVO.setDraws(draws);
        packInfoVO.setDrawPrice(drawPrice);
        packInfoVO.setPackedOriginiumPrice(packedOriginiumPrice);
        packInfoVO.setDrawEfficiency(drawEfficiency);
        packInfoVO.setPackedOriginium(packedOriginium);
        packInfoVO.setPackEfficiency(packEfficiency);

        packInfoVO.setDrawsKernel(drawsKernel);
        packInfoVO.setDrawPriceKernel(drawPriceKernel);
        packInfoVO.setPackedOriginiumPriceKernel(packedOriginiumPriceKernel);
        packInfoVO.setDrawEfficiencyKernel(drawEfficiencyKernel);
        packInfoVO.setPackedOriginiumKernel(packedOriginiumKernel);
        packInfoVO.setPackEfficiencyKernel(packEfficiencyKernel);
    }

    public void uploadPackInfoPageToCos(StageConfigDTO stageConfigDTO){
        List<PackInfoVO> packInfoVOS = listPackInfo(stageConfigDTO);
        Map<String,Object> response = new HashMap<>();
        long timeStamp = System.currentTimeMillis();
        response.put("data",packInfoVOS);
        response.put("update",timeStamp);
        String jsonString = JsonMapper.toJSONString(response);
        tencentCloudService.uploadJsonToCOS(jsonString,"/store/pack/"+timeStamp+".json");
        redisTemplate.opsForValue().set("PackInfoTag",timeStamp);
    }

    public String deletePackInfoById(String id) {
        PackInfo packInfo = new PackInfo();
        packInfo.setId(Long.parseLong(id));
        packInfo.setDeleteFlag(true);
        int delete = packInfoMapper.updateById(packInfo);
        return "删除了" + delete + "条礼包数据";
    }


}
