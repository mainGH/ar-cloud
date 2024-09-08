package org.ar.wallet.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.KycPartnersDTO;
import org.ar.common.pay.req.KycPartnerIdReq;
import org.ar.common.pay.req.KycPartnerListPageReq;
import org.ar.wallet.service.IKycPartnersService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * <p>
 * kyc信息表 前端控制器
 * </p>
 *
 * @author
 * @since 2024-04-26
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping({"/api/v1/kycPartners", "/kycPartners"})
@Api(description = "KYC-Partners")
@ApiIgnore
public class KycPartnersController {

    private final IKycPartnersService kycPartnersService;

    /**
     * 获取KYC Partner列表
     *
     * @return {@link RestResult}<{@link List}<{@link KycPartnersDTO}>>
     */
    @PostMapping("/listPage")
    @ApiOperation(value = "获取KYC Partner列表")
    public RestResult<List<KycPartnersDTO>> listPage(@RequestBody @ApiParam KycPartnerListPageReq kycPartnerListPageReq) {
        PageReturn<KycPartnersDTO> kycPartnersDTOPageReturn = kycPartnersService.listPage(kycPartnerListPageReq);
        return RestResult.page(kycPartnersDTOPageReturn);
    }


    /**
     * 删除
     *
     * @return RestResult
     */
    @PostMapping("/delete")
    @ApiOperation(value = "删除")
    public RestResult listPage(@RequestBody @ApiParam KycPartnerIdReq req) {
        boolean delete = kycPartnersService.delete(req);
        return delete ? RestResult.ok() : RestResult.failed();
    }


}
