package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.enums.RecordType;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.HttpRequestUtil;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import com.lhs.entity.dto.survey.OperatorProgressionDataDTO;
import com.lhs.entity.dto.survey.OperatorProgressionStatisticalResultDTO;
import com.lhs.entity.po.survey.*;

import com.lhs.entity.vo.survey.OperatorProgressionStatisticalResultVOV2;
import com.lhs.mapper.survey.OperatorProgressionDataMapper;
import com.lhs.mapper.survey.OperatorProgressionStatisticalResultMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OperatorProgressionStatisticsService {


    private final OperatorProgressionStatisticalResultMapper operatorProgressionStatisticalResultMapper;


    private final OperatorProgressionDataMapper operatorProgressionDataMapper;


    private final IdGenerator idGenerator;


    public OperatorProgressionStatisticsService(OperatorProgressionStatisticalResultMapper operatorProgressionStatisticalResultMapper,
                                                OperatorProgressionDataMapper operatorProgressionDataMapper) {

        this.operatorProgressionStatisticalResultMapper = operatorProgressionStatisticalResultMapper;

        this.operatorProgressionDataMapper = operatorProgressionDataMapper;

        this.idGenerator = new IdGenerator(1L);

    }


    /**
     * 统计干员练度数据（V2）
     * <p>
     * 以 ak_uid 游标分页全量遍历 operator_progression_data，按记录上传时间过滤出有效样本，
     * 汇总各干员的拥有数、有效样本数以及精英化/技能/模组的分布，
     * 最终以 JSON 形式写入 id 为 20260101 的统计数据记录（存在则更新）
     *
     * @param archived 归档标记，当前实现未使用
     */
    public void statisticsOperatorProgressionDataV2(Boolean archived) {
        Logger.info("干员练度数据统计任务开始执行");
        HashMap<String, Date> operatorUpdateTime = new HashMap<>();
        // 定义格式化器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        String response = HttpRequestUtil.get("https://ark.yituliu.cn/json/operator_update_time.json", new HashMap<>());
        Iterator<Map.Entry<String, JsonNode>> fields = JsonMapper.parseJSONObject(response).fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();
            String key = next.getKey();
            JsonNode value = next.getValue();
            String updateTime = value.get("updateTime").asText();
            LocalDateTime localDateTime = LocalDateTime.parse(updateTime, formatter);
            Date date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
            operatorUpdateTime.put(key, date);
        }

        // 各干员的有效样本数：key 为干员 charId，value 为上传时间不早于该干员更新时间的记录数
        HashMap<String, Integer> sampleSizeMap = new HashMap<>();

        // 收集所有记录的创建时间（毫秒时间戳），循环结束后统一排序并用二分统计样本数。
        // 这样避免在每条记录上遍历全部干员，把复杂度从 O(记录数 × 干员数) 降到 O(N log N + 干员数 log N)
        List<Long> createTimeList = new ArrayList<>();

        //干员练度统计数据统计结果
        Map<String, OperatorProgressionStatisticalResultDTO> collect = new HashMap<>();

        int count = 0;

        List<OperatorProgressionData> operatorProgressionDataList;

        // 游标分页的起始游标：ak_uid 均为非空字符串，空串可匹配到第一条记录
        String lastAkUid = "";
        for (int i = 0; i < 300; i++) {

            // 按 ak_uid 升序游标分页读取，每批最多 1000 条，避免 limit offset 深分页逐页扫描
            operatorProgressionDataList = operatorProgressionDataMapper.getOperatorProgressionData(lastAkUid, 1000);

            if (operatorProgressionDataList.isEmpty()) {
                break;
            }

            // 以本批最后一条的 ak_uid 作为下一批的游标，保证遍历不重不漏（依赖 ak_uid 为主键、唯一）
            lastAkUid = operatorProgressionDataList.get(operatorProgressionDataList.size() - 1).getAkUid();

            count += operatorProgressionDataList.size();


            //循环统计干员练度
            for (OperatorProgressionData operatorProgressionData : operatorProgressionDataList) {
                Date createTime = operatorProgressionData.getCreateTime();
                // 记录本次上传时间，循环结束后统一统计各干员的有效样本数（见下方二分统计）
                createTimeList.add(createTime.getTime());
                String operatorProgression = operatorProgressionData.getOperatorProgression();
                //将json文本转为集合
                List<OperatorProgressionDataDTO> dataDTOList = JsonMapper.parseJSONArray(operatorProgression, new TypeReference<>() {
                });


                //循环每个账号的干员练度
                for (OperatorProgressionDataDTO progressionDataDTO : dataDTOList) {
                    //先判断统计结果是否有这个干员
                    OperatorProgressionStatisticalResultDTO operatorProgressionStatisticalResultDTO = collect.get(progressionDataDTO.getCharId());
                    //没有的话先创建一个对象
                    if (operatorProgressionStatisticalResultDTO == null) {
                        operatorProgressionStatisticalResultDTO = new OperatorProgressionStatisticalResultDTO();
                    }


                    operatorProgressionStatisticalResultDTO.increaseSampleSize();

                    operatorProgressionStatisticalResultDTO.increaseOwn();


                    operatorProgressionStatisticalResultDTO.setCharId(progressionDataDTO.getCharId());
                    operatorProgressionStatisticalResultDTO.mergeElite(progressionDataDTO.getElite());
                    operatorProgressionStatisticalResultDTO.mergeSkill1(progressionDataDTO.getSkill1());
                    operatorProgressionStatisticalResultDTO.mergeSkill2(progressionDataDTO.getSkill2());
                    operatorProgressionStatisticalResultDTO.mergeSkill3(progressionDataDTO.getSkill3());
                    operatorProgressionStatisticalResultDTO.mergeModA(progressionDataDTO.getModA());
                    operatorProgressionStatisticalResultDTO.mergeModB(progressionDataDTO.getModB());
                    operatorProgressionStatisticalResultDTO.mergeModX(progressionDataDTO.getModX());
                    operatorProgressionStatisticalResultDTO.mergeModY(progressionDataDTO.getModY());
                    operatorProgressionStatisticalResultDTO.mergeModD(progressionDataDTO.getModD());
                    collect.put(progressionDataDTO.getCharId(), operatorProgressionStatisticalResultDTO);
                }
            }

            Logger.info("当前批次数据游标：" + lastAkUid);
        }

        // 统计各干员的有效样本数：effectiveSample(charId) = 上传时间不早于该干员更新时间的记录数。
        // 原实现是"每条记录遍历全部干员累加"，这里做等价变形为"对每个干员统计满足条件的记录数"：
        // 先把所有记录的创建时间升序排序，再用二分定位第一个不早于干员更新时间的记录，
        // 其后的记录数量即为该干员的有效样本数，结果与原实现完全一致
        long[] createTimeArray = new long[createTimeList.size()];
        for (int i = 0; i < createTimeArray.length; i++) {
            createTimeArray[i] = createTimeList.get(i);
        }
        Arrays.sort(createTimeArray);
        for (Map.Entry<String, Date> entry : operatorUpdateTime.entrySet()) {
            Date updateTime = entry.getValue();
            // 干员更新时间缺失时跳过，不参与样本统计
            if (updateTime == null) {
                continue;
            }
            // 二分定位第一个创建时间不早于干员更新时间的记录下标
            int index = lowerBound(createTimeArray, updateTime.getTime());
            // 下标及其后的记录均计入样本；index 等于数组长度表示无记录满足条件，
            // 此时不写入 map，保持原有逻辑（后续按总记录数 count 兜底）
            if (index < createTimeArray.length) {
                sampleSizeMap.put(entry.getKey(), createTimeArray.length - index);
            }
        }

        List<OperatorProgressionStatisticalResultDTO> list = new ArrayList<>();
        for (OperatorProgressionStatisticalResultDTO dto : collect.values()) {
            Integer i = sampleSizeMap.get(dto.getCharId());

            dto.setSampleSize(i == null ? count : i);
            list.add(dto);
        }


        LambdaQueryWrapper<OperatorProgressionStatisticalResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OperatorProgressionStatisticalResult::getId, 20260101);
        boolean exists = operatorProgressionStatisticalResultMapper.exists(queryWrapper);
        OperatorProgressionStatisticalResult operatorProgressionStatisticalResult = new OperatorProgressionStatisticalResult();
        operatorProgressionStatisticalResult.setSampleSize(count);
        operatorProgressionStatisticalResult.setId(20260101L);
        operatorProgressionStatisticalResult.setRecordType(RecordType.DISPLAY.code());
        operatorProgressionStatisticalResult.setCreateTime(new Date());
        String jsonString = JsonMapper.toJSONString(list);
        operatorProgressionStatisticalResult.setStatisticalResult(jsonString);
        if (exists) {
            operatorProgressionStatisticalResultMapper.updateById(operatorProgressionStatisticalResult);
        } else {
            operatorProgressionStatisticalResultMapper.insert(operatorProgressionStatisticalResult);
        }

        Logger.info("本次统计干员练度的抽样人数为：" + count + "人次");

    }


    /**
     * 二分查找第一个大于等于目标值的位置（下界）
     * <p>
     * 用于统计"上传时间不早于干员更新时间"的记录数：返回下标 index 后，
     * 区间 [index, array.length) 内的元素均满足条件，数量为 array.length - index
     *
     * @param array  已按升序排列的数组
     * @param target 目标值
     * @return 第一个大于等于目标值的下标，若所有元素都小于目标值则返回数组长度
     */
    private static int lowerBound(long[] array, long target) {
        int low = 0;
        int high = array.length;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (array[mid] < target) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }


    /**
     * 干员信息统计
     *
     * @return 成功消息
     */
    @RedisCacheable(key = "Survey:OperatorProgressionStatistics", timeout = 1200)
    public OperatorProgressionStatisticalResultVOV2 getOperatorProgressionStatisticalResultV2() {
        LambdaUpdateWrapper<OperatorProgressionStatisticalResult> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(OperatorProgressionStatisticalResult::getId, 20260101);
        OperatorProgressionStatisticalResult operatorProgressionStatisticalResult = operatorProgressionStatisticalResultMapper.selectOne(queryWrapper);
        if(operatorProgressionStatisticalResult == null){
            throw new ServiceException(ResultCode.DATA_NONE);
        }
        List<OperatorProgressionStatisticalResultDTO> progressionStatisticalResultDTOList = JsonMapper
                .parseJSONArray(operatorProgressionStatisticalResult.getStatisticalResult(), new TypeReference<>() {
                });

        OperatorProgressionStatisticalResultVOV2 operatorProgressionStatisticalResultVOV2 = new OperatorProgressionStatisticalResultVOV2();
        operatorProgressionStatisticalResultVOV2.setSampleSize(operatorProgressionStatisticalResult.getSampleSize());
        operatorProgressionStatisticalResultVOV2.setRecordType(RecordType.DISPLAY.code());
        operatorProgressionStatisticalResultVOV2.setCreateTime(operatorProgressionStatisticalResult.getCreateTime().getTime());
        operatorProgressionStatisticalResultVOV2.setResult(progressionStatisticalResultDTOList);
        return operatorProgressionStatisticalResultVOV2;
    }


    public void archivedOperatorProgressionResult() {

        // 获取今天的开始时间和结束时间
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date endOfDay = calendar.getTime();

        LambdaUpdateWrapper<OperatorProgressionStatisticalResult> existQueryWrapper = new LambdaUpdateWrapper<>();
        existQueryWrapper.eq(OperatorProgressionStatisticalResult::getRecordType, RecordType.ARCHIVED.code())
                .ge(OperatorProgressionStatisticalResult::getCreateTime, startOfDay)
                .le(OperatorProgressionStatisticalResult::getCreateTime, endOfDay);

        boolean exists = operatorProgressionStatisticalResultMapper.exists(existQueryWrapper);
        if (exists) {
            Logger.info("干员携带率统计结果今日已归档");
            return;
        }

        LambdaUpdateWrapper<OperatorProgressionStatisticalResult> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(OperatorProgressionStatisticalResult::getRecordType, RecordType.DISPLAY.code())
                .orderByDesc(OperatorProgressionStatisticalResult::getCreateTime);
        List<OperatorProgressionStatisticalResult> list = operatorProgressionStatisticalResultMapper.selectList(queryWrapper);
        OperatorProgressionStatisticalResult operatorProgressionStatisticalResult = list.get(0);
        operatorProgressionStatisticalResult.setRecordType(RecordType.ARCHIVED.code());
        operatorProgressionStatisticalResult.setId(idGenerator.nextId());

        int i = operatorProgressionStatisticalResultMapper.insert(operatorProgressionStatisticalResult);
        Logger.info("干员携带率统计结果归档成功" + i + "条");

    }

   

    public void backup() {

    }


}
