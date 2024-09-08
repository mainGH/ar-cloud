package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.KycBankDTO;
import org.ar.common.pay.req.KycBankIdReq;
import org.ar.common.pay.req.KycBankListPageReq;
import org.ar.common.pay.req.KycBankReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

/**
 * @author admin
 * @date 2024/4/27 11:17
 */
@FeignClient(value = "ar-wallet", contextId = "kyc-bank")
public interface KycBankClient {

    @PostMapping("/api/v1/kycBank/listPage")
    RestResult<List<KycBankDTO>> listPage(KycBankListPageReq req);

    @PostMapping("/api/v1/kycBank/deleteKycBank")
    RestResult deleteKycBank(KycBankIdReq req);

    @PostMapping("/api/v1/kycBank/addKycBank")
    RestResult<KycBankDTO> addKycBank(KycBankReq req);

    @PostMapping("/api/v1/kycBank/updateKycBank")
    RestResult<KycBankDTO> updateKycBank(KycBankReq req);

    @PostMapping("/api/v1/kycBank/getBankCodeList")
    RestResult<List<String>> getBankCodeList();
}
