package com.lhs.controller;

import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.Result;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.po.item.PackItem;
import com.lhs.entity.vo.dev.LoginVo;
import com.lhs.entity.vo.dev.PageViewStatisticsVo;
import com.lhs.entity.vo.dev.VisitsTimeVo;
import com.lhs.entity.vo.item.PackInfoVO;
import com.lhs.entity.vo.item.ActivityStoreDataVO;
import com.lhs.service.dev.AdminService;
import com.lhs.service.user.VisitsService;
import com.lhs.service.item.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "一图流后台")
public class AdminController {

    private final StoreService storeService;

    private final AdminService adminService;

    private final VisitsService visitsService;

    public AdminController(StoreService storeService, AdminService adminService, VisitsService visitsService) {
        this.storeService = storeService;
        this.adminService = adminService;
        this.visitsService = visitsService;
    }

    @GetMapping("/")
    public Result<String> startStatus() {

        return Result.success("后端启动成功");
    }

    @GetMapping("/visits/page")
    public Result<Object> updatePageVisits(@RequestParam String path) {
        visitsService.updatePageVisits(path);
        return Result.success();
    }

    @GetMapping("/visits/save")
    public Result<Object> savePageVisits() {
        visitsService.savePageVisits();
        return Result.success();
    }

    @Operation(summary = "管理登录发送验证码")
    @PostMapping("/auth/email/verificationCode")
    public Result<Object> emailSendCode(@RequestBody LoginVo loginVo) {
        adminService.emailSendCode(loginVo);
        return Result.success("已发送验证码到您的邮箱,5分钟过期");
    }

    @Operation(summary = "管理者登录")
    @PostMapping("/auth/login")
    public Result<Map<String,Object>> loginAndToken(@RequestBody LoginVo loginVo) {

        return Result.success(adminService.login(loginVo));
    }

    @Operation(summary = "获取管理者信息")
    @GetMapping("/auth/developer/info")
    public Result<Map<String,Object>> getDeveloperInfo(@RequestParam("token") String token) {

        return Result.success(adminService.getDeveloperInfo(token));
    }

    @Operation(summary = "检查管理者登录状态")
    @GetMapping("/auth/login/checkToken")
    public Result<Boolean> loginAndCheckToken(HttpServletRequest request) {
        String token = request.getHeader("token");
        if(token ==null) throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        Boolean status  =  adminService.checkToken(token);
        return Result.success(status);
    }

    @Operation(summary = "更新商店礼包")
    @PostMapping("/admin/store/pack/update")
    public Result<PackInfoVO> updateStageResult(@RequestBody PackInfoVO packInfoVO) {
        PackInfoVO pack = storeService.updateStorePackById(packInfoVO);
        return Result.success(pack);
    }

    @Operation(summary = "上传礼包图片")
    @PostMapping("/admin/store/pack/upload/image")
    public Result<Object> uploadImage(@RequestParam("file") MultipartFile file,@RequestParam("id") Long id) {
        storeService.uploadPackImage(file,id);
        return Result.success();
    }

    @Operation(summary = "根据id获取礼包")
    @GetMapping("/admin/store/pack")
    public Result<PackInfoVO> updateStageResult(@RequestParam(required = false, defaultValue = "1") String id) {
        PackInfoVO pack = storeService.getPackById(id);
        return Result.success(pack);
    }


    @Operation(summary = "获取全部礼包")
    @GetMapping("/dev/store/pack")
    public Result<List<PackInfoVO>> getPackList(){
        return Result.success(storeService.listAllPackInfo());
    }

    @Operation(summary = "更新礼包材料表")
    @PostMapping("/admin/item/update")
    public Result<PackItem> saveOrUpdatePackItem(@RequestBody PackItem newPackItem){
        PackItem packItem = storeService.saveOrUpdatePackItem(newPackItem);
        return Result.success(packItem);
    }

    @Operation(summary = "删除礼包材料")
    @GetMapping("/admin/item/delete")
    public Result<Object> deletePackItem(@RequestParam String id){
        storeService.deletePackItemById(id);
        return Result.success();
    }

    @Operation(summary = "清除礼包缓存数据")
    @GetMapping("/admin/pack/clearCache")
    public Result<Object> clearPackCache(){
        String message = storeService.clearPackCache();
        return Result.success(message);
    }


    @PostMapping("/admin1/view/statistics")
    public Result<List<PageViewStatisticsVo>> queryVisits(@RequestBody VisitsTimeVo visitsTimeVo) {
        List<PageViewStatisticsVo> pageViewStatisticsVos = visitsService.getVisits(visitsTimeVo);
        return Result.success(pageViewStatisticsVos);
    }


    @Operation(summary = "更新活动商店性价比(新")
    @PostMapping("/admin/store/act/update")
    public Result<Object> updateActStoreByActName(HttpServletRequest request, @RequestBody ActivityStoreDataVO activityStoreDataVo) {
        Boolean level = adminService.developerLevel(request);
        String message = storeService.updateActivityStoreDataByActivityName(activityStoreDataVo, level);
        return Result.success(message);
    }

    @Operation(summary = "上传商店背景图片")
    @PostMapping("/admin/store/activity/uploadImage")
    public Result<String> uploadActivityImage(@RequestParam("file") MultipartFile file) {
        return Result.success(storeService.uploadActivityBackgroundImage(file));
    }


    @Operation(summary = "活动商店历史数据")
    @GetMapping("/store/act/history")
    public Result<List<ActivityStoreDataVO>> selectActStoreHistory() {
        List<ActivityStoreDataVO> list =   storeService.getActivityStoreHistoryData();
        return Result.success(list);
    }

}
