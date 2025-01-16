package com.lhs.mapper.rogueSeed;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.dto.rogueSeed.RogueSeedIdAndTypeDTO;
import com.lhs.entity.po.rogue.RogueSeed;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RogueSeedMapper extends BaseMapper<RogueSeed> {


    List<RogueSeedIdAndTypeDTO> listRogueSeedIdAndType();
}
