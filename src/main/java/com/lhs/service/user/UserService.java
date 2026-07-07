package com.lhs.service.user;

import com.lhs.entity.dto.user.LoginDataDTO;
import com.lhs.entity.dto.user.UpdateUserDataDTO;
import com.lhs.entity.po.user.UserInfo;
import com.lhs.entity.vo.survey.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;

public interface UserService {

    /**
     * 用户注册
     *
     * @param httpServletRequest HTTP请求对象
     * @param loginDataDTO       用户修改的信息
     * @return 用户状态信息
     */
    HashMap<String, Object> registerV3(HttpServletRequest httpServletRequest, LoginDataDTO loginDataDTO);

    /**
     * 用户登录
     *
     * @param httpServletRequest HTTP请求对象
     * @param loginDataDTO       用户修改的信息
     * @return 用户状态信息
     */
    HashMap<String, Object> loginV3(HttpServletRequest httpServletRequest, LoginDataDTO loginDataDTO);

    /**
     * 从请求中提取token
     *
     * @param request HTTP请求对象
     * @return token
     */
    String extractToken(HttpServletRequest request);

    /**
     * 检查用户登录状态
     *
     * @param httpServletRequest HTTP请求对象
     * @return 是否登录
     */
    Boolean checkUserLoginStatus(HttpServletRequest httpServletRequest);

    /**
     * 通过token获取用户信息
     *
     * @param token 用户登录后获得的凭证
     * @return 用户信息
     */
    UserInfoVO getUserInfoVOByToken(String token);

    /**
     * 通过token获取用户数据内的信息
     *
     * @param token 用户登录后获得的凭证
     * @return 用户信息
     */
    UserInfo getUserInfoPOByToken(String token);

    /**
     * 通过HttpServletRequest获取token，根据token拿到用户信息
     *
     * @param httpServletRequest HTTP请求对象
     * @return 用户信息
     */
    UserInfoVO getUserInfoVOByHttpServletRequest(HttpServletRequest httpServletRequest);

    /**
     * 通过HttpServletRequest获取token，根据token拿到用户id，如果没有token则查看请求头是否含有前端传来的临时uid，如果有则返回，没有则根据ip生成一个临时uid
     *
     * @param httpServletRequest HTTP请求对象
     * @return 用户信息
     */
    Long getUidByHttpServletRequest(HttpServletRequest httpServletRequest);

    /**
     * 通过HttpServletRequest获取token，根据token拿到用户信息
     *
     * @param httpServletRequest HTTP请求对象
     * @return 用户信息
     */
    UserInfo getUserInfoPOByHttpServletRequest(HttpServletRequest httpServletRequest);

    /**
     * 更新用户信息
     *
     * @param httpServletRequest HTTP请求对象
     * @param updateUserDataDto  要更新的内容
     * @return 用户信息
     */
    UserInfoVO updateUserData(HttpServletRequest httpServletRequest, UpdateUserDataDTO updateUserDataDto);

    /**
     * 备份用户信息到腾讯云COS
     */
    void backupUserInfo();

    /**
     * 找回账号
     *
     * @param loginDataDTO 找回所需的内容
     * @return 临时凭证
     */
    HashMap<String, String> retrieveAccount(LoginDataDTO loginDataDTO);

    /**
     * 重设密码
     *
     * @param httpServletRequest HTTP请求对象
     * @param loginDataDTO       找回所需的内容
     * @return 用户凭证
     */
    HashMap<String, String> resetPassword(HttpServletRequest httpServletRequest, LoginDataDTO loginDataDTO);

    /**
     * 生成第三方API访问token（读/写权限），token存储在Redis中，30天过期
     *
     * @param httpServletRequest HTTP请求对象
     * @param scope              token权限范围：read-只读，write-读写
     * @return API访问token
     */
    String generateOpenApiToken(HttpServletRequest httpServletRequest, String scope);

    /**
     * 校验第三方API token并返回用户uid
     *
     * @param token         API token
     * @param requiredScope 需要的权限：read-只读，write-读写（write权限token可执行read操作）
     * @return 用户uid
     */
    Long validateOpenApiToken(String token, String requiredScope);

    /**
     * 用户登出，删除Redis中的登录token
     *
     * @param httpServletRequest HTTP请求对象
     */
    void logout(HttpServletRequest httpServletRequest);

    /**
     * 删除第三方API token
     *
     * @param httpServletRequest HTTP请求对象
     * @param scope              权限范围：read 或 write
     */
    void deleteOpenApiToken(HttpServletRequest httpServletRequest, String scope);
}
