package com.lhs.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.stage.Item;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemMapper extends BaseMapper<Item> {



}
