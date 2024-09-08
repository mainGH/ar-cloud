package org.ar.pay.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.ResultCode;
import org.ar.common.core.utils.AssertUtil;
import org.ar.common.pay.dto.UserAuthDTO;
import org.ar.common.web.utils.UserContext;
import org.ar.pay.entity.*;
import org.ar.pay.mapper.MerchantInfoMapper;
import org.ar.pay.req.MerchantInfoReq;
import org.ar.pay.service.*;
import org.ar.pay.util.PageUtils;
import org.ar.pay.vo.MerchantInfoVo;
import org.ar.pay.vo.MerchantNameListVo;
import org.h2.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class MerchantInfoServiceImpl extends ServiceImpl<MerchantInfoMapper, MerchantInfo> implements IMerchantInfoService {
    private final PasswordEncoder passwordEncoder;
    private final IMerchantRoleService merchantRoleService;
    private final ISysRoleMenuService roleMenuService;
    private final ISysRolePermissionService rolePermissionService;
    private final ISysPermissionService permissionService;
//    private final AdminMapStruct adminMapStruct;
//    private final ICollectionOrderService collectionOrderService;
//    private final IPaymentOrderService paymentOrderService;


    public void createMerchantInfo(MerchantInfo merchantInfo) {
        String passwd = passwordEncoder.encode(GlobalConstants.USER_DEFAULT_PASSWORD);
        SysUser sysUser = new SysUser();
        //BeanUtils.copyProperties(req, sysUser);
        sysUser.setPassword(passwd);
        //save(sysUser);
        // 维护角色关系
        saveUserRoles(merchantInfo.getRoleIds(), sysUser.getId());

    }

    @Override
    public PageReturn<MerchantInfo> listPage(MerchantInfoReq req) {
        Page<MerchantInfo> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<MerchantInfo> lambdaQuery = lambdaQuery();
        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getCode())) {
            lambdaQuery.eq(MerchantInfo::getCode, req.getCode());
        }
        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getUsername())) {
            lambdaQuery.eq(MerchantInfo::getUsername, req.getUsername());
        }
        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<MerchantInfo> records = page.getRecords();
        return PageUtils.flush(page, records);
    }

    @Override
    public List<MerchantInfo> getAllMerchantByStatus() {
        LambdaQueryChainWrapper<MerchantInfo> lambdaQuery = lambdaQuery();
        List<MerchantInfo> list = lambdaQuery().eq(MerchantInfo::getStatus, "1").list();
        return list;
    }

    @Override
    public UserAuthDTO getByUsername(String username) {
        UserAuthDTO userAuthDTO = this.baseMapper.getByUsername(username);
        return userAuthDTO;
    }

    private void saveUserRoles(List<Long> roleIds, Long userId) {
        if (CollectionUtil.isNotEmpty(roleIds)) {
            List<MerchantRole> merchantRole = new ArrayList<>();
            roleIds.forEach(roleId -> {
                merchantRole.add(new MerchantRole(userId, roleId));
            });
            merchantRoleService.saveBatch(merchantRole);
        }
    }

    @Override
    public boolean getIp(String code, String addr) {
        MerchantInfo merchantInfo = lambdaQuery().eq(MerchantInfo::getCode, code).one();
        if (merchantInfo != null && !StringUtils.isNullOrEmpty(merchantInfo.getWhiteList())) {
            String whiteStr = merchantInfo.getWhiteList();
            List<String> list = Arrays.asList(",");
            if (list.contains(addr)) return true;
        }
        return false;
    }

    @Override
    public MerchantInfo getMerchantInfoByCode(String code) {
        MerchantInfo merchantInfo = lambdaQuery().eq(MerchantInfo::getCode, code).one();
        return merchantInfo;
    }

    /*
     * 获取当前商户信息
     * */
    @Override
    public MerchantInfoVo currentMerchantInfo() {
        Long currentUserId = UserContext.getCurrentUserId();
        AssertUtil.notEmpty(currentUserId, ResultCode.RELOGIN);
        MerchantInfo merchantInfo = userDetail(currentUserId);
        AssertUtil.notEmpty(merchantInfo, ResultCode.USERNAME_OR_PASSWORD_ERROR);
        // 查询绑定的菜单
        List<Long> roleIds = merchantRoleService.selectRoleIds(currentUserId);
        List<SysRoleMenu> roleMenus = roleMenuService.lambdaQuery().in(SysRoleMenu::getRoleId, roleIds).list();
        merchantInfo.setMenuIds(roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList()));
        // 获取权限
        List<SysRolePermission> rolePermissions = rolePermissionService.lambdaQuery().in(SysRolePermission::getRoleId, roleIds).list();
        merchantInfo.setPermissions(Arrays.asList());
        if (CollectionUtil.isNotEmpty(rolePermissions)) {
            List<Long> permissionIds = rolePermissions.stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
            List<SysPermission> sysPermissions = permissionService.lambdaQuery().in(SysPermission::getId, permissionIds).list();
            if (CollectionUtil.isNotEmpty(sysPermissions)) {
                List<String> btnSigns = sysPermissions.stream().map(SysPermission::getBtnSign).collect(Collectors.toList());
                merchantInfo.setPermissions(btnSigns);
            }
        }

        MerchantInfoVo merchantInfoVo = new MerchantInfoVo();
        BeanUtils.copyProperties(merchantInfo, merchantInfoVo);

        //查询该商户总代收金额
//        QueryWrapper<CollectionOrder> queryAllCollection = new QueryWrapper<>();
//        queryAllCollection.select("sum(amount) as allCollectionAmount");
//        queryAllCollection.eq("merchant_code", merchantInfo.getCode());
//        queryAllCollection.eq("order_status", 2);
//        CollectionOrder collectionOrder = collectionOrderService.getOne(queryAllCollection);
//        if (collectionOrder != null) {
//            merchantInfoVo.setAllCollectionAmount(collectionOrder.getAllCollectionAmount());
//        }
//        System.out.println("getAllCollectionAmount: " + merchantInfoVo.getAllCollectionAmount());


        //查询该商户总代付金额
//        QueryWrapper<PaymentOrder> queryAllTransferAmount = new QueryWrapper<>();
//        queryAllTransferAmount.select("sum(settlement_amount) as allTransferAmount");
//        queryAllTransferAmount.eq("merchant_code", merchantInfo.getCode());
//        queryAllTransferAmount.eq("order_status", 2);
//        PaymentOrder paymentOrder = paymentOrderService.getOne(queryAllTransferAmount);
//        if (paymentOrder != null) {
//            merchantInfoVo.setAllTransferAmount(paymentOrder.getAllTransferAmount());
//        }
//        System.out.println("setAllTransferAmount: " + merchantInfoVo.getAllTransferAmount());


        //总下发次数
        merchantInfoVo.setTransferCount("100");


        //总下发金额
        merchantInfoVo.setTransferAmount("100");

        return merchantInfoVo;
    }

    @Override
    public MerchantInfo userDetail(Long userId) {
        MerchantInfo merchantInfo = lambdaQuery().eq(MerchantInfo::getId, userId).one();
        AssertUtil.notEmpty(merchantInfo, ResultCode.USERNAME_OR_PASSWORD_ERROR);

        // 查询绑定的角色IDs
        List<Long> roleIds = merchantRoleService.selectRoleIds(userId);
        merchantInfo.setRoleIds(roleIds);
        return merchantInfo;
    }

    /*
     * 根据商户号获取md5Key
     * */
    @Override
    public String getMd5KeyByCode(String merchantCode) {
        QueryWrapper<MerchantInfo> MerchantInfoQueryWrapper = new QueryWrapper<>();
        MerchantInfoQueryWrapper.select("md5_key").eq("code", merchantCode);
        return getOne(MerchantInfoQueryWrapper).getMd5Key();
    }

    /*
     * 获取商户名称列表
     * */
    @Override
    public List<MerchantNameListVo> getMerchantNameList() {
        //获取当前商户id
        Long currentUserId = UserContext.getCurrentUserId();
        //查询当前商户名称和商户号
        QueryWrapper<MerchantInfo> merchantInfoQueryWrapper = new QueryWrapper<>();
        merchantInfoQueryWrapper.select("code", "username").eq("id", currentUserId);
        List<Map<String, Object>> maps = listMaps(merchantInfoQueryWrapper);
        ArrayList<MerchantNameListVo> merchantNameListVos = new ArrayList<>();

        for (Map<String, Object> map : maps) {
            MerchantNameListVo merchantNameListVo = new MerchantNameListVo();
            merchantNameListVo.setValue(String.valueOf(map.get("code")));
            merchantNameListVo.setLabel(String.valueOf(map.get("username")));
            merchantNameListVos.add(merchantNameListVo);
        }
        return merchantNameListVos;
    }

    /*
     * 根据商户号查询支付费率和代付费率
     * */
    @Override
    public Map<String, Object> getRateByCode(String merchantCode) {
        QueryWrapper<MerchantInfo> merchantInfoQueryWrapper = new QueryWrapper<>();
        merchantInfoQueryWrapper.select("pay_rate", "transfer_rate").eq("code", merchantCode);
        return getMap(merchantInfoQueryWrapper);
    }

}
