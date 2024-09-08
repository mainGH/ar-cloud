package org.ar.wallet.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.result.KycRestResult;
import org.ar.wallet.req.KycPartnerReq;
import org.ar.wallet.req.KycSellReq;
import org.ar.wallet.req.LinkKycPartnerReq;
import org.ar.wallet.service.IKycCenterService;
import org.ar.wallet.vo.KycBanksVo;
import org.ar.wallet.vo.KycPartnersVo;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/kycCenter")
@Api(description = "KYC控制器")
public class KycCenterController {

    private final IKycCenterService kycCenterService;

    /**
     * 获取KYC Partner列表
     *
     * @return {@link KycRestResult}<{@link List}<{@link KycPartnersVo}>>
     */
    @GetMapping("/getKycPartners")
    @ApiOperation(value = "获取KYC Partner列表")
    public KycRestResult<List<KycPartnersVo>> getKycPartners() {
        return kycCenterService.getKycPartners();
    }


    /**
     * 添加 KYC Partner
     *
     * @param kycPartnerReq
     * @param request
     * @return {@link KycRestResult}
     */
    @PostMapping("/addKycPartner")
    @ApiOperation(value = "添加 KYC Partner")
    public KycRestResult addKycPartner(@RequestBody @ApiParam KycPartnerReq kycPartnerReq, HttpServletRequest request) {
        return kycCenterService.addKycPartner(kycPartnerReq, request);
    }


    /**
     * 连接KYC
     *
     * @param linkKycPartnerReq
     * @param request
     * @return {@link KycRestResult}
     */
    @PostMapping("/linkKycPartner")
    @ApiOperation(value = "连接KYC Partner")
    public KycRestResult linkKycPartner(@RequestBody @ApiParam LinkKycPartnerReq linkKycPartnerReq, HttpServletRequest request) {
        return kycCenterService.linkKycPartner(linkKycPartnerReq, request);
    }


    /**
     * 获取银行列表
     *
     * @return {@link KycRestResult}<{@link List}<{@link KycBanksVo}>>
     */
    @GetMapping("/getBanks")
    @ApiOperation(value = "获取 KYC银行列表")
    public KycRestResult<List<KycBanksVo>> getBanks() {
        return kycCenterService.getBanks();
    }


    /**
     * 开始卖出
     *
     * @param kycSellReq
     * @param request
     * @return {@link KycRestResult}
     */
    @PostMapping("/startSell")
    @ApiOperation(value = "开始卖出")
    public KycRestResult startSell(@RequestBody @ApiParam KycSellReq kycSellReq, HttpServletRequest request) {
        return kycCenterService.startSell(kycSellReq, request);
    }


    /**
     * 停止卖出
     *
     * @param kycSellReq
     * @param request
     * @return {@link KycRestResult}
     */
    @PostMapping("/stopSell")
    @ApiOperation(value = "停止卖出")
    public KycRestResult stopSell(@RequestBody @ApiParam KycSellReq kycSellReq, HttpServletRequest request) {
        return kycCenterService.stopSell(kycSellReq, request);
    }
}


