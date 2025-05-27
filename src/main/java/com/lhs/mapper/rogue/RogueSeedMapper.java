package com.lhs.mapper.rogue;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.dto.rogue.RogueSeedIdAndTypeDTO;
import com.lhs.entity.po.rogue.RogueSeed;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RogueSeedMapper extends BaseMapper<RogueSeed> {


    List<RogueSeedIdAndTypeDTO> listRogueSeedIdAndType();
}
