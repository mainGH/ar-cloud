package org.ar.pay.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.web.utils.UserContext;
import org.ar.pay.entity.MerchantInfo;
import org.ar.pay.entity.MerchantRole;
import org.ar.pay.req.MerchantInfoReq;
import org.ar.pay.service.IMerchantInfoService;
import org.ar.pay.service.IMerchantRoleService;
import org.ar.pay.vo.MerchantInfoVo;
import org.ar.pay.vo.MerchantNameListVo;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/merchantInfo")
@Api(description = "商户控制器")
public class MerchantInfoController {

    private final IMerchantInfoService merchantInfoService;
    private final PasswordEncoder passwordEncoder;
    private final IMerchantRoleService merchantRoleService;


    @PostMapping("/createMerchantInfo")
    @ApiOperation(value = "创建商户")
    public RestResult<MerchantInfo> save(@RequestBody @ApiParam MerchantInfoVo merchantInfoVo) {
        String passwd = passwordEncoder.encode(GlobalConstants.USER_DEFAULT_PASSWORD);
        MerchantInfo merchantInfo = new MerchantInfo();
        BeanUtils.copyProperties(merchantInfoVo, merchantInfo);
        merchantInfo.setPassword(passwd);
        merchantInfo.setStatus("1");
        merchantInfo.setDeleted("0");
        merchantInfoService.save(merchantInfo);
        saveMerchantRoles(merchantInfo.getRoleIds(), merchantInfo.getId());
        return RestResult.ok();
    }

    @ApiOperation(value = "保存商户角色")
    private void saveMerchantRoles(List<Long> roleIds, Long userId) {
        if (CollectionUtil.isNotEmpty(roleIds)) {
            List<MerchantRole> merchantRole = new ArrayList<>();
            roleIds.forEach(roleId -> {
                merchantRole.add(new MerchantRole(userId, roleId));
            });
            merchantRoleService.saveBatch(merchantRole);
        }
    }

    @PostMapping("/update")
    @ApiOperation(value = "更新商户信息")
    public RestResult update(@RequestBody @ApiParam MerchantInfoReq merchantInfoReq) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (!merchantInfoReq.getId().equals(currentUserId)) return RestResult.ok("必须当前用户才能修改");
        MerchantInfo merchantInfo = new MerchantInfo();
        BeanUtils.copyProperties(merchantInfoReq, merchantInfo);
        boolean su = merchantInfoService.updateById(merchantInfo);
        return RestResult.ok();

    }

    @PostMapping("/listpage")
    @ApiOperation(value = "获取商户列表")
    public RestResult list(@RequestBody @ApiParam MerchantInfoReq merchantInfoReq) {
        PageReturn<MerchantInfo> payConfigPage = merchantInfoService.listPage(merchantInfoReq);
        return RestResult.page(payConfigPage);
    }

    @GetMapping("/current")
    @ApiOperation(value = "获取当前商户信息")
    public RestResult<MerchantInfoVo> currentMerchantInfo() {
        MerchantInfoVo merchantInfo = merchantInfoService.currentMerchantInfo();
        return RestResult.ok(merchantInfo);
    }

    @GetMapping("/merchantNameList")
    @ApiOperation(value = "获取商户名称列表")
    public RestResult merchantNameList() {
        List<MerchantNameListVo> payConfigPage = merchantInfoService.getMerchantNameList();
        return RestResult.ok(payConfigPage);
    }

}
