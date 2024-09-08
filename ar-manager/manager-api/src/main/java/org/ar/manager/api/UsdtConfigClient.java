package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.C2cConfigDTO;
import org.ar.common.pay.dto.UsdtConfigDTO;
import org.ar.common.pay.req.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


/**
 * @author Admin
 */
@FeignClient(value = "ar-wallet", contextId = "usdt-config")
public interface UsdtConfigClient {


    /**
     *
     * @param
     * @return
     */
    @PostMapping("/api/v1/usdtConfig/listpage")
    RestResult<List<UsdtConfigDTO>> listpage(@RequestBody UsdtConfigPageReq req);

    /**
     *
     * @param req
     * @return
     */
    @PostMapping("/api/v1/usdtConfig/create")
    RestResult<UsdtConfigDTO> create(@RequestBody UsdtConfigCreateReq req);



    @PostMapping("/api/v1/usdtConfig/update")
    RestResult<UsdtConfigDTO> update(@RequestBody UsdtConfigReq req);


    @PostMapping("/api/v1/usdtConfig/changeStatus")
    RestResult<UsdtConfigDTO> changeStatus(@RequestBody UsdtConfigQueryReq req);

    @PostMapping("/api/v1/usdtConfig/getInfo")
    RestResult<UsdtConfigDTO> getInfo(@RequestBody UsdtConfigIdReq req);


    @PostMapping("/api/v1/usdtConfig/delete")
    RestResult delete(@RequestBody UsdtConfigIdReq req);


}
