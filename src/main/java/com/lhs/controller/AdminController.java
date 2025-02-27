package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.material.PackInfoDTO;
import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.po.admin.LogInfo;
import com.lhs.entity.po.material.ItemCustom;
import com.lhs.entity.vo.dev.LoginVo;
import com.lhs.entity.vo.dev.PageViewStatisticsVo;
import com.lhs.entity.vo.dev.VisitsTimeVo;
import com.lhs.entity.vo.material.PackInfoVO;
import com.lhs.entity.vo.material.ActivityStoreDataVO;
import com.lhs.service.admin.AdminService;
import com.lhs.service.admin.ImageInfoService;
import com.lhs.service.admin.LogService;
import com.lhs.service.material.PackInfoService;
import com.lhs.service.user.VisitsService;
import com.lhs.service.material.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@Tag(name = "一图流后台")
public class AdminController {

    private final StoreService storeService;
    private final PackInfoService packInfoService;

    private final AdminService adminService;

    private final VisitsService visitsService;

    private final ImageInfoService imageInfoService;

    private final LogService logService;

    public AdminController(StoreService storeService, AdminService adminService, VisitsService visitsService,
                           ImageInfoService imageInfoService, PackInfoService packInfoService, LogService logService) {
        this.storeService = storeService;
        this.adminService = adminService;
        this.visitsService = visitsService;
        this.imageInfoService = imageInfoService;
        this.packInfoService = packInfoService;
        this.logService = logService;
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

    @PostMapping("/log/collect")
    public Result<String> collectLog(@RequestBody LogInfo logInfo){
        logService.saveLog(logInfo);
        return Result.success();
    }

    @GetMapping("/visits/save")
    public Result<Object> savePageVisits() {
        visitsService.savePageVisits();
        return Result.success();
    }

    @Operation(summary = "管理登录发送验证码")
    @PostMapping("/dev/email/verificationCode")
    public Result<Object> emailSendCode(@RequestBody LoginVo loginVo) {
        adminService.emailSendCode(loginVo);
        return Result.success("已发送验证码到您的邮箱,5分钟过期");
    }

    @Operation(summary = "管理者登录")
    @PostMapping("/dev/login")
    public Result<Map<String,Object>> loginAndToken(@RequestBody LoginVo loginVo) {

        return Result.success(adminService.login(loginVo));
    }

    @Operation(summary = "获取管理者信息")
    @GetMapping("/dev/developer/info")
    public Result<Map<String,Object>> getDeveloperInfo(@RequestParam("token") String token) {
        return Result.success(adminService.getDeveloperInfo(token));
    }


    @GetMapping("/admin/cache/keys")
    public Result<Map<String, Object>> cacheKeys(HttpServletRequest httpServletRequest) {
        return Result.success(adminService.getCacheKeys());
    }

    @GetMapping("/admin/cache/delete")
    public Result<String> deleteCacheKey(HttpServletRequest httpServletRequest,@RequestParam("key") String key) {
        return Result.success(adminService.deleteCacheKey(key));
    }

    @Operation(summary = "更新商店礼包")
    @PostMapping("/admin/store/pack/update")
    public Result<String> updateStageResult(HttpServletRequest httpServletRequest,@RequestBody PackInfoDTO packInfoDTO) {
        return Result.success( packInfoService.saveOrUpdatePackInfo(packInfoDTO));
    }

    @Operation(summary = "根据id获取礼包")
    @GetMapping("/admin/store/pack")
    public Result<PackInfoVO> updateStageResult(HttpServletRequest httpServletRequest,@RequestParam("id") String id) {
        PackInfoVO pack = packInfoService.getPackById(id);
        return Result.success(pack);
    }

    @Operation(summary = "删除礼包材料")
    @GetMapping("/admin/store/pack/delete")
    public Result<Object> deletePackInfo(HttpServletRequest httpServletRequest,@RequestParam String id){
        return Result.success(packInfoService.deletePackInfoById(id));
    }


    @Operation(summary = "获取全部礼包")
    @GetMapping("/dev/store/pack")
    public Result<List<PackInfoVO>> getPackList(){
        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        return Result.success(packInfoService.listAllPackInfo(stageConfigDTO));
    }


    @Operation(summary = "更新礼包材料表")
    @PostMapping("/admin/item/update")
    public Result<ItemCustom> saveOrUpdatePackItem(HttpServletRequest httpServletRequest,@RequestBody ItemCustom newItemCustom){
        ItemCustom itemCustom = packInfoService.saveOrUpdatePackItem(newItemCustom);
        return Result.success(itemCustom);
    }

    @Operation(summary = "删除礼包材料")
    @GetMapping("/admin/item/delete")
    public Result<String> deletePackItem(HttpServletRequest httpServletRequest,@RequestParam String id){

        return Result.success(packInfoService.deletePackItemById(id));
    }




    @PostMapping("/admin/view/statistics")
    public Result<List<PageViewStatisticsVo>> queryVisits(@RequestBody VisitsTimeVo visitsTimeVo) {
        List<PageViewStatisticsVo> pageViewStatisticsVos = visitsService.getVisits(visitsTimeVo);
        return Result.success(pageViewStatisticsVos);
    }


    @Operation(summary = "更新活动商店性价比(新")
    @PostMapping("/admin/store/act/update")
    public Result<Object> updateActStoreByActName(HttpServletRequest request, @RequestBody ActivityStoreDataVO activityStoreDataVo) {

        String message = storeService.updateActivityStoreDataByActivityName(activityStoreDataVo);
        return Result.success(message);
    }


    @Operation(summary = "活动商店历史数据")
    @GetMapping("/store/act/history")
    public Result<List<ActivityStoreDataVO>> selectActStoreHistory() {
        List<ActivityStoreDataVO> list =   storeService.getActivityStoreHistoryData();
        return Result.success(list);
    }

    @Operation(summary = "上传图片服务")
    @PostMapping("/admin/upload/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file,@RequestParam("imageName") String imageName) {
        imageInfoService.saveImage(file,"image/",imageName);
        return Result.success("上传成功");
    }

    @Operation(summary = "批量上传图片服务")
    @PostMapping("/admin/upload/images")
    public Result<String> uploadImageFiles(@RequestParam("files") List<MultipartFile> files, @RequestParam("path") String path) {

        return Result.success(imageInfoService.saveImageFiles(files,path));
    }

    @Operation(summary = "获取礼包自定义材料表")
    @GetMapping("/store/pack/item/list")
    public Result<List<ItemCustom>> getItemList() {
        List<ItemCustom> itemCustomList = packInfoService.listCustomItem(new StageConfigDTO());
        return Result.success(itemCustomList);
    }
}
