package com.lhs.service.survey;

import com.lhs.common.context.UserContext;
import com.lhs.common.util.IdGenerator;
import com.lhs.entity.dto.survey.WarehouseInventoryAPIParams;
import com.lhs.entity.dto.user.AkPlayerBindInfoDTO;
import com.lhs.entity.po.survey.WarehouseInfo;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.survey.WarehouseInfoMapper;
import com.lhs.mapper.survey.service.WarehouseInfoMapperService;
import com.lhs.mapper.user.AkPlayerBindInfoMapper;
import com.lhs.service.user.BindService;
import com.lhs.service.user.OAuthUserService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WarehouseInfoService {

    private final WarehouseInfoMapper warehouseInfoMapper;


    private final BindService bindService;


    private final IdGenerator idGenerator;


    public WarehouseInfoService(WarehouseInfoMapper warehouseInfoMapper,
                                BindService bindService) {
        this.warehouseInfoMapper = warehouseInfoMapper;

        this.bindService = bindService;

        this.idGenerator = new IdGenerator(1L);
    }


    public Map<String, Object> saveWarehouseInventoryInfo(WarehouseInventoryAPIParams params) {
        //一图流用户凭证
        String token = params.getToken();
        //明日方舟玩家uid
        String akUid = params.getAkUid();
        //玩家仓库信息
        List<WarehouseInfo> warehouseInfoList = params.getList();
        //一图流用户信息
        Long uid = UserContext.getUid();


        //数据库id
        //最新数据的标记id
        long lastDataId = idGenerator.nextId();

        AkPlayerBindInfoDTO akPlayerBindInfoDTO = new AkPlayerBindInfoDTO();
        akPlayerBindInfoDTO.setAkUid(akUid);
        akPlayerBindInfoDTO.setWarehouseInfoId(lastDataId);
        bindService.saveSklandBindingAndPlayerInfo(uid, akPlayerBindInfoDTO);

        //当前导入时间的时间戳
        long timeStamp = System.currentTimeMillis();
        //为仓库信息写入数据库id、时间戳、最新数据的标记id、明日方舟玩家uid，一图流用户id
        for (WarehouseInfo warehouseInfo : warehouseInfoList) {
            warehouseInfo.setId(idGenerator.nextId());
            warehouseInfo.setUid(uid);
            warehouseInfo.setLastDataId(lastDataId);
            warehouseInfo.setAkUid(akUid);
            warehouseInfo.setUpdateTime(timeStamp);
        }

        warehouseInfoMapper.insertBatch(warehouseInfoList);


        Map<String, Object> result = new HashMap<>();

        return result;
    }
}
