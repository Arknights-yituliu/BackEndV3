package com.lhs.service.survey.impl;

import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.TimeUtil;
import com.lhs.entity.dto.survey.CharacterTableOperatorDTO;
import com.lhs.entity.dto.survey.OperatorProgressionDataDTO;
import com.lhs.entity.dto.survey.OperatorProgressionDataV2DTO;
import com.lhs.entity.dto.user.UcAkAccountVO;
import com.lhs.entity.dto.user.UcOperatorListVO;
import com.lhs.entity.dto.user.UcOperatorVO;
import com.lhs.entity.po.survey.OperatorProgressionData;
import com.lhs.mapper.survey.OperatorProgressionDataMapper;
import com.lhs.service.survey.OperatorDataService;
import com.lhs.service.user.UcGameDataClient;
import com.lhs.service.util.CharacterTableService;
import com.lhs.service.util.TencentCloudService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 干员练度服务实现
 * <p>
 * 本地导入与回读链路（森空岛导入、手动导入、按绑定关系回读）已随对应接口下线一并移除，
 * 本类只保留两条能力：
 * <ol>
 *   <li>定时任务使用的本地练度数据备份；</li>
 *   <li>第三方读取接口所需的 UC 令牌转发与结构富化。</li>
 * </ol>
 */
@Service
public class OperatorDataServiceImpl implements OperatorDataService {

    private final CharacterTableService characterTableService;

    private final OperatorProgressionDataMapper operatorProgressionDataMapper;

    private final TencentCloudService tencentCloudService;

    private final UcGameDataClient ucGameDataClient;

    public OperatorDataServiceImpl(OperatorProgressionDataMapper operatorProgressionDataMapper,
            TencentCloudService tencentCloudService,
            UcGameDataClient ucGameDataClient,
            CharacterTableService characterTableService) {
        this.operatorProgressionDataMapper = operatorProgressionDataMapper;
        this.tencentCloudService = tencentCloudService;
        this.ucGameDataClient = ucGameDataClient;
        this.characterTableService = characterTableService;
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

    @Override
    public List<OperatorProgressionDataV2DTO> openApiGetOperatorData(HttpServletRequest httpServletRequest) {
        // 前端传来的第三方令牌由酸橙云签发保管，本地不再保存、也不再做存在性校验，直接以其为凭证请求 UC
        String token = httpServletRequest.getHeader("Authorization");
        List<UcOperatorVO> ucOperatorList = listUcOperators(token);

        // 使用启动时缓存的character_table数据进行转换
        Map<String, CharacterTableOperatorDTO> characterTableMap = characterTableService.getCharacterTable();
        return transformToV2DTO(toProgressionDataList(ucOperatorList), characterTableMap);
    }

    /**
     * 取用户最近导入数据的游戏账号，并读取该账号的干员全量数据
     *
     * @param accessToken UC access_token
     * @return 干员列表；未绑定账号或无干员数据时为空列表
     */
    private List<UcOperatorVO> listUcOperators(String accessToken) {
        // UC 侧按更新时间倒序返回绑定账号，取首条即最近导入的账号
        List<UcAkAccountVO> accounts = ucGameDataClient.listAkAccounts(accessToken);
        if (accounts == null || accounts.isEmpty()) {
            return new ArrayList<>();
        }
        UcOperatorListVO operatorList = ucGameDataClient.listOperators(accessToken, accounts.get(0).getAkUid());
        if (operatorList == null || operatorList.getItems() == null) {
            return new ArrayList<>();
        }
        return operatorList.getItems();
    }

    /**
     * 将 UC 干员数据映射为本地练度数据结构，以复用角色表富化逻辑
     *
     * @param ucOperatorList UC 侧干员列表
     * @return 本地练度数据列表
     */
    private List<OperatorProgressionDataDTO> toProgressionDataList(List<UcOperatorVO> ucOperatorList) {
        List<OperatorProgressionDataDTO> resultList = new ArrayList<>(ucOperatorList.size());
        for (UcOperatorVO source : ucOperatorList) {
            OperatorProgressionDataDTO dto = new OperatorProgressionDataDTO();
            dto.setCharId(source.getId());
            // UC 返回的均为该账号已持有的干员，own 恒为 true
            dto.setOwn(true);
            dto.setRarity(source.getRarity());
            dto.setLevel(source.getLevel());
            dto.setElite(source.getEvolvePhase());
            dto.setMainSkill(source.getMainSkillLevel());
            dto.setSkill1(source.getSkill1());
            dto.setSkill2(source.getSkill2());
            dto.setSkill3(source.getSkill3());
            dto.setModX(source.getEquipX());
            dto.setModY(source.getEquipY());
            dto.setModD(source.getEquipD());
            dto.setModA(source.getEquipA());
            dto.setModB(source.getEquipB());
            dto.setPotential(source.getPotentialRank());
            resultList.add(dto);
        }
        return resultList;
    }

    /**
     * 将原始干员练度数据转换为富化后的V2格式
     *
     * @param rawDataList       原始干员练度数据
     * @param characterTableMap 角色表（charId → 干员数据）
     * @return 富化后的干员练度数据列表
     */
    private List<OperatorProgressionDataV2DTO> transformToV2DTO(
            List<OperatorProgressionDataDTO> rawDataList,
            Map<String, CharacterTableOperatorDTO> characterTableMap) {

        List<OperatorProgressionDataV2DTO> resultList = new ArrayList<>();

        for (OperatorProgressionDataDTO raw : rawDataList) {
            OperatorProgressionDataV2DTO dto = new OperatorProgressionDataV2DTO();
            dto.setId(raw.getCharId());
            dto.setLevel(raw.getLevel());
            dto.setEvolvePhase(raw.getElite());
            dto.setMainSkillLevel(raw.getMainSkill());
            dto.setPotentialRank(raw.getPotential());

            // 从缓存的角色表中获取该干员的名称、技能和模组信息
            CharacterTableOperatorDTO charData = characterTableMap.get(raw.getCharId());
            if (charData != null) {
                dto.setName(charData.getName());
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
     * @param charData 角色表中缓存的干员数据
     * @param raw      原始练度数据
     * @return 技能信息列表
     */
    private List<OperatorProgressionDataV2DTO.SkillInfo> buildSkillList(
            CharacterTableOperatorDTO charData, OperatorProgressionDataDTO raw) {

        List<OperatorProgressionDataV2DTO.SkillInfo> skillList = new ArrayList<>();
        List<CharacterTableOperatorDTO.Skill> skills = charData.getSkills();
        if (skills == null || skills.isEmpty()) {
            return skillList;
        }

        // 将DB中的skill1、skill2、skill3按顺序映射
        Integer[] skillLevels = { raw.getSkill1(), raw.getSkill2(), raw.getSkill3() };

        for (int i = 0; i < skills.size(); i++) {
            String skillId = skills.get(i).getSkillId();
            Integer level = (i < skillLevels.length && skillLevels[i] != null) ? skillLevels[i] : 0;
            skillList.add(new OperatorProgressionDataV2DTO.SkillInfo(skillId, level));
        }

        return skillList;
    }

    /**
     * 构建模组列表（模组ID + 类型 + 等级），仅包含角色表中存在的模组分支
     *
     * @param charData 角色表中缓存的干员数据
     * @param raw      原始练度数据
     * @return 模组信息列表
     */
    private List<OperatorProgressionDataV2DTO.EquipInfo> buildEquipList(
            CharacterTableOperatorDTO charData, OperatorProgressionDataDTO raw) {

        List<OperatorProgressionDataV2DTO.EquipInfo> equipList = new ArrayList<>();
        List<CharacterTableOperatorDTO.Equip> equips = charData.getEquip();
        if (equips == null || equips.isEmpty()) {
            return equipList;
        }

        // 构建typeName2→uniEquipId的映射
        Map<String, String> typeToEquipId = new LinkedHashMap<>();
        for (CharacterTableOperatorDTO.Equip equip : equips) {
            String typeName2 = equip.getTypeName2();
            String uniEquipId = equip.getUniEquipId();
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
