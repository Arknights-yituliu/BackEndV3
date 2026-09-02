package com.lhs.service.survey.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.context.UserContext;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.survey.OperatorProgressionDataDTO;
import com.lhs.entity.dto.survey.OperatorProgressionDataV2DTO;
import com.lhs.entity.dto.survey.ManualOperatorDataDTO;
import com.lhs.entity.dto.survey.PlayerInfoDTO;
import com.lhs.entity.dto.user.AkPlayerBindInfoDTO;
import com.lhs.entity.dto.user.OpenApiPermission;
import com.lhs.entity.po.survey.*;

import com.lhs.entity.po.user.UserExternalAccountBinding;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.survey.OperatorProgressionDataMapper;
import com.lhs.mapper.survey.OperatorProgressionManualDataMapper;
import com.lhs.mapper.user.UserExternalAccountBindingMapper;
import com.lhs.service.survey.OperatorDataService;
import com.lhs.service.survey.WarehouseInfoService;
import com.lhs.service.user.BindService;

import com.lhs.service.user.OpenApiService;
import com.lhs.service.util.TencentCloudService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class OperatorDataServiceImpl implements OperatorDataService {

    private final RedisTemplate<String, Object> redisTemplate;

    
    private final OpenApiService openApiService;
    private final BindService bindService;

    private final IdGenerator idGenerator;

    private final OperatorProgressionDataMapper operatorProgressionDataMapper;
    private final OperatorProgressionManualDataMapper operatorProgressionManualDataMapper;

    private final TencentCloudService tencentCloudService;
    private final UserExternalAccountBindingMapper userExternalAccountBindingMapper;

    public OperatorDataServiceImpl(RedisTemplate<String, Object> redisTemplate,
            OpenApiService openApiService, BindService bindService,
            OperatorProgressionDataMapper operatorProgressionDataMapper,
            OperatorProgressionManualDataMapper operatorProgressionManualDataMapper,
            WarehouseInfoService warehouseInfoService,
            TencentCloudService tencentCloudService,
            UserExternalAccountBindingMapper userExternalAccountBindingMapper) {
        this.redisTemplate = redisTemplate;
      
        this.openApiService = openApiService;
        this.bindService = bindService;
        this.operatorProgressionDataMapper = operatorProgressionDataMapper;
        this.operatorProgressionManualDataMapper = operatorProgressionManualDataMapper;
        this.tencentCloudService = tencentCloudService;
        this.userExternalAccountBindingMapper = userExternalAccountBindingMapper;
        this.idGenerator = new IdGenerator(1L);
    }

    /**
     * 获取干员角色表（初次读取时从文件系统加载并缓存到Redis，后续从Redis读取）
     *
     * @return 干员角色表，key为charId，value为干员详情JsonNode
     */
    private Map<String, JsonNode> getCharacterTable() {
        // 尝试从Redis获取缓存的JSON字符串
        Object cached = redisTemplate.opsForValue().get(RedisKeyUtil.characterTable("2026-07-08 14:20"));
        String jsonText;
        if (cached != null) {
            jsonText = cached.toString();
        } else {
            // 初次读取，从文件系统加载并缓存到Redis
            jsonText = FileUtil.read(ConfigUtil.DataFilePath + "character_table_simple.v2.json");
            if (jsonText == null) {
                return new HashMap<>();
            }
            redisTemplate.opsForValue().set(RedisKeyUtil.characterTable("2026-07-08 14:20"), jsonText);
            Logger.info("character_table_simple.v2.json 已加载并缓存到Redis");
        }

        // 解析JSON为Map
        Map<String, JsonNode> resultMap = new HashMap<>();
        JsonNode root = JsonMapper.parseJSONObject(jsonText);
        if (root != null) {
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                resultMap.put(entry.getKey(), entry.getValue());
            }
        }
        return resultMap;
    }

    @Override
    public Object importSKLandPlayerInfoV3(HttpServletRequest httpServletRequest, PlayerInfoDTO playerInfoDTO) {

        Long uid = UserContext.getUid();

        // 防止用户多次点击上传
        Boolean done = redisTemplate.opsForValue().setIfAbsent(RedisKeyUtil.surveyOperatorUploadInterval(uid),
                "done", 5, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(done)) {
            throw new ServiceException(ResultCode.NOT_REPEAT_REQUESTS);
        }

        List<OperatorProgressionDataDTO> operatorDataList = playerInfoDTO.getOperatorDataList();
        String akUid = playerInfoDTO.getUid();

        AkPlayerBindInfoDTO akPlayerBindInfoDTO = new AkPlayerBindInfoDTO();
        akPlayerBindInfoDTO.setAkNickName(playerInfoDTO.getNickName());
        akPlayerBindInfoDTO.setAkUid(akUid);
        akPlayerBindInfoDTO.setChannelName(playerInfoDTO.getChannelName());
        akPlayerBindInfoDTO.setChannelMasterId(playerInfoDTO.getChannelMasterId());
        bindService.saveSklandBindingAndPlayerInfo(uid, akPlayerBindInfoDTO);

        return saveOperatorData(akUid, operatorDataList);
    }

    @Override
    @Transactional
    public Map<String, Object> importManualOperatorData(ManualOperatorDataDTO manualOperatorDataDTO) {
        Long uid = UserContext.getUid();
        checkOperatorDataUploadInterval(uid);

        List<OperatorProgressionDataDTO> operatorDataList = manualOperatorDataDTO.getOperatorDataList();
        if (operatorDataList == null) {
            throw new ServiceException(ResultCode.PARAM_IS_BLANK, "operatorDataList is required");
        }

        // 手动数据没有可验证的游戏 UID，固定使用当前一图流用户 UID，防止客户端伪造归属。
        return saveManualOperatorData(String.valueOf(uid), operatorDataList);
    }

    private void checkOperatorDataUploadInterval(Long uid) {
        // 同一用户短时间内只允许一次导入，避免并发请求同时覆盖同一份练度数据。
        Boolean done = redisTemplate.opsForValue().setIfAbsent(RedisKeyUtil.surveyOperatorUploadInterval(uid),
                "done", 5, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(done)) {
            throw new ServiceException(ResultCode.NOT_REPEAT_REQUESTS);
        }
    }

    /**
     * 保存干员数据
     *
     * @param akUid                          明日方舟玩家uid
     * @param operatorProgressionDataDTOList 干员练度调查表
     * @return 成功信息
     */
    private Map<String, Object> saveOperatorData(String akUid,
            List<OperatorProgressionDataDTO> operatorProgressionDataDTOList) {

        // 本次修改影响的数据行数
        int affectedRows = 0;

        // 循环上传的干员练度
        for (OperatorProgressionDataDTO operatorProgressionDataDTO : operatorProgressionDataDTOList) {
            // 更新数据条数
            operatorProgressionDataDTO.setOwn(true);
            checkOperatorDataValidity(operatorProgressionDataDTO);
            affectedRows++; // 新增数据条数
        }

        LambdaQueryWrapper<OperatorProgressionData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OperatorProgressionData::getAkUid, akUid);
        boolean exists = operatorProgressionDataMapper.exists(queryWrapper);

        OperatorProgressionData operatorProgressionData = new OperatorProgressionData();
        operatorProgressionData.setAkUid(akUid);
        operatorProgressionData.setOperatorProgression(JsonMapper.toJSONString(operatorProgressionDataDTOList));
        operatorProgressionData.setCreateTime(new Date());

        if (exists) {
            operatorProgressionDataMapper.updateById(operatorProgressionData);
        } else {
            operatorProgressionDataMapper.insert(operatorProgressionData);
        }

        Date date = new Date();
        // 更新用户最后一次上传时间
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("affectedRows", affectedRows);
        hashMap.put("updateTime", simpleDateFormat.format(date));

        return hashMap;
    }

    /**
     * 覆盖保存手动录入的干员数据。该表以一图流用户 UID 为主键，与森空岛来源的数据隔离。
     */
    private Map<String, Object> saveManualOperatorData(String akUid,
            List<OperatorProgressionDataDTO> operatorProgressionDataDTOList) {
        for (OperatorProgressionDataDTO operatorProgressionDataDTO : operatorProgressionDataDTOList) {
            operatorProgressionDataDTO.setOwn(true);
            checkOperatorDataValidity(operatorProgressionDataDTO);
        }

        OperatorProgressionManualData existingData = operatorProgressionManualDataMapper.getTimeInfoByAkUid(akUid);
        Date now = new Date();
        OperatorProgressionManualData operatorProgressionData = new OperatorProgressionManualData();
        operatorProgressionData.setAkUid(akUid);
        operatorProgressionData.setOperatorProgression(JsonMapper.toJSONString(operatorProgressionDataDTOList));
        operatorProgressionData.setCreateTime(existingData == null ? now : existingData.getCreateTime());
        operatorProgressionData.setUpdateTime(now);

        if (existingData != null) {
            operatorProgressionManualDataMapper.updateById(operatorProgressionData);
        } else {
            operatorProgressionManualDataMapper.insert(operatorProgressionData);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("affectedRows", operatorProgressionDataDTOList.size());
        result.put("updateTime", new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(now));
        return result;
    }

    /**
     * 对新老干员数据进行检查，是否有非法数据
     *
     * @param operatorProgressionDataDTO 新干员数据
     */
    private void checkOperatorDataValidity(OperatorProgressionDataDTO operatorProgressionDataDTO) {

        // 精英化阶段小于2 不能专精和开模组
        if (operatorProgressionDataDTO.getElite() < 2) {
            operatorProgressionDataDTO.setSkill1(0);
            operatorProgressionDataDTO.setSkill2(0);
            operatorProgressionDataDTO.setSkill3(0);
            operatorProgressionDataDTO.setModX(0);
            operatorProgressionDataDTO.setModY(0);
            operatorProgressionDataDTO.setModD(0);
        }

        if (operatorProgressionDataDTO.getRarity() < 6) {
            if (!operatorProgressionDataDTO.getCharId().contains("amiya")) {
                operatorProgressionDataDTO.setSkill3(0);
            }
        }

        if (!operatorProgressionDataDTO.getOwn()) {
            operatorProgressionDataDTO.setMainSkill(0);
            operatorProgressionDataDTO.setPotential(0);
            operatorProgressionDataDTO.setSkill1(0);
            operatorProgressionDataDTO.setSkill2(0);
            operatorProgressionDataDTO.setSkill3(0);
            operatorProgressionDataDTO.setModX(0);
            operatorProgressionDataDTO.setModY(0);
            operatorProgressionDataDTO.setModD(0);
        }

        if (operatorProgressionDataDTO.getMainSkill() == null) {
            operatorProgressionDataDTO.setMainSkill(1);
        }

        if (operatorProgressionDataDTO.getModD() == null) {
            operatorProgressionDataDTO.setModD(0);
        }

    }

    @Override
    public Result<Object> operatorDataReset(String token) {

        return Result.success("重置了" + "条数据");
    }

    @Override
    public List<OperatorProgressionDataDTO> listOperatorProgressionData() {
        Long uid = UserContext.getUid();
        List<UserExternalAccountBinding> bindings = userExternalAccountBindingMapper.selectList(
                new LambdaQueryWrapper<UserExternalAccountBinding>()
                        .eq(UserExternalAccountBinding::getUid, uid));

        // 绑定表的更新时间也会被仓库导入修改，不能用于判断练度数据的新旧。
        OperatorProgressionData latestSklandData = null;
        for (UserExternalAccountBinding binding : bindings) {
            OperatorProgressionData sklandData = operatorProgressionDataMapper.selectById(binding.getAkUid());
            if (sklandData == null || sklandData.getOperatorProgression() == null) {
                continue;
            }
            if (latestSklandData == null || isLater(sklandData.getCreateTime(), latestSklandData.getCreateTime())) {
                latestSklandData = sklandData;
            }
        }

        String manualAkUid = String.valueOf(uid);
        OperatorProgressionManualData manualTimeData = operatorProgressionManualDataMapper
                .getTimeInfoByAkUid(manualAkUid);
        Date manualUpdateTime = manualTimeData == null ? null
                : (manualTimeData.getUpdateTime() == null ? manualTimeData.getCreateTime() : manualTimeData.getUpdateTime());

        // 仅在手动数据更晚时读取其完整 JSON，避免不必要地加载 LONGTEXT 字段。
        if (manualTimeData != null && (latestSklandData == null
                || isLater(manualUpdateTime, latestSklandData.getCreateTime()))) {
            OperatorProgressionManualData manualData = operatorProgressionManualDataMapper.selectById(manualAkUid);
            if (manualData != null && manualData.getOperatorProgression() != null) {
                return JsonMapper.parseJSONArray(manualData.getOperatorProgression(), new TypeReference<>() {
                });
            }
        }

        if (latestSklandData != null) {
            return JsonMapper.parseJSONArray(latestSklandData.getOperatorProgression(), new TypeReference<>() {
            });
        }
        throw new ServiceException(ResultCode.OPERATOR_DATA_NOT_FOUND);
    }

    private boolean isLater(Date candidateTime, Date currentTime) {
        return candidateTime != null && (currentTime == null || candidateTime.after(currentTime));
    }

    @Override
    public void backupOperatorProgressionData() {
        String dayText = TimeUtil.getDayText();
        List<OperatorProgressionData> operatorProgressionDataList;
        for (int i = 0; i < 100; i++) {
            operatorProgressionDataList = operatorProgressionDataMapper.getOperatorProgressionData(i * 2000, 2000);
            if (operatorProgressionDataList.isEmpty()) {
                break;
            }
            tencentCloudService.backupCOS(JsonMapper.toJSONString(operatorProgressionDataList),
                    "/mysql/operatorProgressionData/" + dayText + "/" + i + ".json");
        }
    }

    private Map<String, Object> saveOpenApiOperatorData(Long uid, PlayerInfoDTO playerInfoDTO) {
        String akUid = playerInfoDTO.getUid();
        List<OperatorProgressionDataDTO> operatorDataList = playerInfoDTO.getOperatorDataList();

        // 保存用户与方舟uid的绑定关系
        AkPlayerBindInfoDTO akPlayerBindInfoDTO = new AkPlayerBindInfoDTO();
        akPlayerBindInfoDTO.setAkNickName(playerInfoDTO.getNickName());
        akPlayerBindInfoDTO.setAkUid(akUid);
        akPlayerBindInfoDTO.setChannelName(playerInfoDTO.getChannelName());
        akPlayerBindInfoDTO.setChannelMasterId(playerInfoDTO.getChannelMasterId());

      
        bindService.saveSklandBindingAndPlayerInfo(uid, akPlayerBindInfoDTO);

        return saveOperatorData(akUid, operatorDataList);
    }

    private List<OperatorProgressionDataDTO> getOperatorDataByUid(Long uid) {
        // 查询用户绑定的方舟uid
        LambdaQueryWrapper<UserExternalAccountBinding> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserExternalAccountBinding::getUid, uid)
                .orderByDesc(UserExternalAccountBinding::getUpdateTime);
        List<UserExternalAccountBinding> bindings = userExternalAccountBindingMapper.selectList(queryWrapper);

        if (bindings.isEmpty()) {
            return new ArrayList<>();
        }

        String akUid = bindings.get(0).getAkUid();

        // 查询干员数据
        LambdaQueryWrapper<OperatorProgressionData> dataQueryWrapper = new LambdaQueryWrapper<>();
        dataQueryWrapper.eq(OperatorProgressionData::getAkUid, akUid);
        OperatorProgressionData operatorProgressionData = operatorProgressionDataMapper.selectOne(dataQueryWrapper);

        if (operatorProgressionData == null) {
            return new ArrayList<>();
        }

        return JsonMapper.parseJSONArray(operatorProgressionData.getOperatorProgression(), new TypeReference<>() {
        });
    }

    @Override
    public Map<String, Object> openApiUploadOperatorData(HttpServletRequest httpServletRequest,
            PlayerInfoDTO playerInfoDTO) {
        String token = httpServletRequest.getHeader("Authorization");
        Long uid = openApiService.validateOpenApiToken(token, OpenApiPermission.operatorDataWriteAccess.getCode());
        return saveOpenApiOperatorData(uid, playerInfoDTO);
    }

    @Override
    public List<OperatorProgressionDataV2DTO> openApiGetOperatorData(HttpServletRequest httpServletRequest) {
        String token = httpServletRequest.getHeader("Authorization");
        Long uid = openApiService.validateOpenApiToken(token, OpenApiPermission.operatorDataReadAccess.getCode());
        List<OperatorProgressionDataDTO> rawDataList = getOperatorDataByUid(uid);

        // 使用启动时缓存的character_table数据进行转换
        Map<String, JsonNode> characterTableMap = getCharacterTable();
        return transformToV2DTO(rawDataList, characterTableMap);
    }

    /**
     * 将原始干员练度数据转换为富化后的V2格式
     *
     * @param rawDataList 原始干员练度数据
     * @return 富化后的干员练度数据列表
     */
    private List<OperatorProgressionDataV2DTO> transformToV2DTO(
            List<OperatorProgressionDataDTO> rawDataList,
            Map<String, JsonNode> characterTableMap) {

        List<OperatorProgressionDataV2DTO> resultList = new ArrayList<>();

        for (OperatorProgressionDataDTO raw : rawDataList) {
            OperatorProgressionDataV2DTO dto = new OperatorProgressionDataV2DTO();
            dto.setId(raw.getCharId());
            dto.setLevel(raw.getLevel());
            dto.setEvolvePhase(raw.getElite());
            dto.setMainSkillLevel(raw.getMainSkill());
            dto.setPotentialRank(raw.getPotential());

            // 从缓存的角色表中获取该干员的技能和模组信息
            JsonNode charData = characterTableMap.get(raw.getCharId());
            if (charData != null) {
                dto.setSkills(buildSkillList(charData, raw));
                dto.setEquips(buildEquipList(charData, raw));
            } else {
                dto.setSkills(new ArrayList<>());
                dto.setEquips(new ArrayList<>());
            }

            resultList.add(dto);
        }

        return resultList;
    }

    /**
     * 构建技能列表（skillId + 练度等级），只包含干员实际拥有的技能数量
     *
     * @param charData 角色表中缓存的干员JsonNode数据
     * @param raw      原始练度数据
     * @return 技能信息列表
     */
    private List<OperatorProgressionDataV2DTO.SkillInfo> buildSkillList(
            JsonNode charData, OperatorProgressionDataDTO raw) {

        List<OperatorProgressionDataV2DTO.SkillInfo> skillList = new ArrayList<>();
        JsonNode skillsNode = charData.get("skills");
        if (skillsNode == null || !skillsNode.isArray()) {
            return skillList;
        }

        // 将DB中的skill1、skill2、skill3按顺序映射
        Integer[] skillLevels = { raw.getSkill1(), raw.getSkill2(), raw.getSkill3() };

        for (int i = 0; i < skillsNode.size(); i++) {
            JsonNode skillNode = skillsNode.get(i);
            String skillId = skillNode.has("skillId") ? skillNode.get("skillId").asText() : null;
            Integer level = (i < skillLevels.length && skillLevels[i] != null) ? skillLevels[i] : 0;
            skillList.add(new OperatorProgressionDataV2DTO.SkillInfo(skillId, level));
        }

        return skillList;
    }

    /**
     * 构建模组列表（模组ID + 类型 + 等级），仅包含角色表中存在的模组分支
     *
     * @param charData 角色表中缓存的干员JsonNode数据
     * @param raw      原始练度数据
     * @return 模组信息列表
     */
    private List<OperatorProgressionDataV2DTO.EquipInfo> buildEquipList(
            JsonNode charData, OperatorProgressionDataDTO raw) {

        List<OperatorProgressionDataV2DTO.EquipInfo> equipList = new ArrayList<>();
        JsonNode equipsNode = charData.get("equip");
        if (equipsNode == null || !equipsNode.isArray()) {
            return equipList;
        }

        // 构建typeName2→uniEquipId的映射
        Map<String, String> typeToEquipId = new LinkedHashMap<>();
        for (JsonNode equip : equipsNode) {
            String typeName2 = equip.has("typeName2") ? equip.get("typeName2").asText() : null;
            String uniEquipId = equip.has("uniEquipId") ? equip.get("uniEquipId").asText() : null;
            if (typeName2 != null && uniEquipId != null) {
                typeToEquipId.put(typeName2, uniEquipId);
            }
        }

        // 根据DB中的mod值构建模组列表，只包含角色表中存在的模组类型
        addEquipIfExists(equipList, typeToEquipId, "X", raw.getModX());
        addEquipIfExists(equipList, typeToEquipId, "Y", raw.getModY());
        addEquipIfExists(equipList, typeToEquipId, "D", raw.getModD());
        addEquipIfExists(equipList, typeToEquipId, "A", raw.getModA());
        addEquipIfExists(equipList, typeToEquipId, "B", raw.getModB());

        return equipList;
    }

    /**
     * 如果该模组类型在角色表中存在，则添加到模组列表
     *
     * @param equipList     模组列表
     * @param typeToEquipId 类型→模组ID映射
     * @param type          模组类型（X/Y/D/A/B）
     * @param level         模组等级
     */
    private void addEquipIfExists(List<OperatorProgressionDataV2DTO.EquipInfo> equipList,
            Map<String, String> typeToEquipId,
            String type, Integer level) {
        String uniEquipId = typeToEquipId.get(type);
        if (uniEquipId != null) {
            equipList.add(new OperatorProgressionDataV2DTO.EquipInfo(uniEquipId, type, level != null ? level : 0));
        }
    }

}
