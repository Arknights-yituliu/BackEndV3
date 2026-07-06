package com.lhs.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.user.UserInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserInfoMapper extends BaseMapper<UserInfo> {

    /**
     * 查询所有旧 AES 格式密码的用户（密码不以 $2 开头，即非 bcrypt 哈希）
     */
    @Select("SELECT * FROM user_info WHERE pass_word IS NOT NULL AND pass_word != '' AND pass_word NOT LIKE '$2%'")
    List<UserInfo> selectLegacyPasswordUsers();

    /**
     * 查询旧格式密码用户总数
     */
    @Select("SELECT COUNT(*) FROM user_info WHERE pass_word IS NOT NULL AND pass_word != '' AND pass_word NOT LIKE '$2%'")
    int countLegacyPasswordUsers();

}
