package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.vo.survey.SklandCredTokenVO;
import com.lhs.entity.vo.survey.SklandQrCheckVO;
import com.lhs.entity.vo.survey.SklandQrCreateVO;
import com.lhs.service.survey.SklandHgTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 森空岛（Skland）相关接口控制器
 * <p>
 * 包含：
 * 1. 扫码登录：create 获取二维码 → 前端渲染展示 → 轮询 check 直至 status=0 拿到凭证
 * 2. 鹰角官网 token 换取森空岛 cred/token 凭证
 */
@RestController
@Tag(name = "森空岛相关接口")
public class SklandController {

    private final SklandHgTokenService sklandHgTokenService;

    public SklandController(SklandHgTokenService sklandHgTokenService) {
        this.sklandHgTokenService = sklandHgTokenService;
    }

    /**
     * 通过鹰角官网 token 换取森空岛 cred 和 token
     *
     * @param params 请求体（token：鹰角官网 token）
     * @return 森空岛 cred 和 token
     */
    @Operation(summary = "通过鹰角官网 token 换取森空岛 cred 和 token")
    @PostMapping("/survey/hg/cred-token")
    public Result<SklandCredTokenVO> getCredAndTokenByHgToken(@RequestBody Map<String, String> params) {
        return Result.success(sklandHgTokenService.getCredAndTokenByHgToken(params.get("token")));
    }

    /**
     * 申请森空岛扫码登录二维码
     *
     * @return 扫码会话（scanId + 二维码内容）
     */
    @Operation(summary = "申请森空岛扫码登录二维码")
    @PostMapping("/survey/skland/qr/create")
    public Result<SklandQrCreateVO> createQrLogin() {
        return Result.success(sklandHgTokenService.createQrLogin());
    }

    /**
     * 查询扫码状态并换取森空岛凭证
     * 前端按 1.5~2 秒轮询，status=0 表示完成，可停止轮询
     *
     * @param scanId 扫码会话 ID
     * @return 扫码状态（100/101/102 继续轮询，0 携带 cred/token）
     */
    @Operation(summary = "查询扫码状态并换取森空岛凭证（status=0 即成功）")
    @PostMapping("/survey/skland/qr/check")
    public Result<SklandQrCheckVO> checkQrLogin(@RequestParam String scanId) {
        return Result.success(sklandHgTokenService.checkQrLogin(scanId));
    }
}
