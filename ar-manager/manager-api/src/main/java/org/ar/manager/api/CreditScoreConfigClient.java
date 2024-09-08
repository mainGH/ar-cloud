package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.CreditScoreConfigDTO;
import org.ar.common.pay.req.CreditScoreConfigListPageReq;
import org.ar.common.pay.req.CreditScoreConfigUpdateReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author admin
 * @date 2024/4/9 15:21
 */
@FeignClient(value = "ar-wallet", contextId = "credit-score-config")
public interface CreditScoreConfigClient {

    @PostMapping("/api/v1/creditScoreConfig/listPage")
    RestResult<List<CreditScoreConfigDTO>> listPage(@RequestBody CreditScoreConfigListPageReq req);

    @PostMapping("/api/v1/creditScoreConfig/updateScore")
    RestResult<CreditScoreConfigDTO> updateScore(@RequestBody CreditScoreConfigUpdateReq req);
}
