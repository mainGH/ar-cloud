package org.ar.manager.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.KycPartnersDTO;
import org.ar.common.pay.req.KycPartnerIdReq;
import org.ar.common.pay.req.KycPartnerListPageReq;
import org.ar.manager.api.KycPartnersClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
@RequestMapping({"/api/v1/kycPartnersAdmin", "/kycPartnersAdmin"})
@Api(description = "KYC-Partners")
public class KycPartnersController {

    private final KycPartnersClient kycPartnersClient;

    /**
     * 获取KYC Partner列表
     *
     * @return {@link RestResult}<{@link List}<{@link KycPartnersDTO}>>
     */
    @PostMapping("/listPage")
    @ApiOperation(value = "获取KYC Partner列表")
    public RestResult<List<KycPartnersDTO>> listPage(@RequestBody @ApiParam KycPartnerListPageReq kycPartnerListPageReq) {
        return kycPartnersClient.listPage(kycPartnerListPageReq);
    }


    /**
     * 删除
     *
     * @return RestResult
     */
    @PostMapping("/delete")
    @ApiOperation(value = "删除")
    public RestResult delete(@RequestBody @ApiParam KycPartnerIdReq req) {
        return kycPartnersClient.delete(req);
    }


}
