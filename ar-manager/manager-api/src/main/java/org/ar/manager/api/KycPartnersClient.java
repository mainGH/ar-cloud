package org.ar.manager.api;

import io.swagger.annotations.ApiParam;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.KycPartnersDTO;
import org.ar.common.pay.req.KycPartnerIdReq;
import org.ar.common.pay.req.KycPartnerListPageReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author admin
 * @date 2024/4/27 10:26
 */
@FeignClient(value = "ar-wallet", contextId = "kyc-partners")
public interface KycPartnersClient {

    @PostMapping("/api/v1/kycPartners/listPage")
    RestResult<List<KycPartnersDTO>>  listPage(@RequestBody @ApiParam KycPartnerListPageReq kycPartnerListPageReq);

    @PostMapping("/api/v1/kycPartners/delete")
    RestResult delete(@RequestBody @ApiParam KycPartnerIdReq req);
}
