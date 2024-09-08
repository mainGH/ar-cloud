package org.ar.manager.api;

import org.ar.common.core.page.PageRequest;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.BannerInfoDTO;
import org.ar.common.pay.dto.BannerInfoListPageDTO;
import org.ar.common.pay.req.BannerInfoReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * @author Admin
 */
@FeignClient(value = "ar-wallet", contextId = "banner")
public interface BannerInfoFeignClient {

    /**
     * 新增 Banner
     *
     * @param req
     * @return {@link Boolean}
     */
    @PostMapping("/api/v1/banner/createBanner")
    RestResult createBanner(@RequestBody BannerInfoReq req);


    /**
     * 根据ID获取 Banner信息
     *
     * @param id
     * @return {@link RestResult}<{@link BannerInfoDTO}>
     */
    @GetMapping("/api/v1/banner/{id}")
    RestResult<BannerInfoDTO> getBannerById(@PathVariable("id") Long id);


    /**
     * 更新 Banner信息
     *
     * @param id
     * @param req
     * @return {@link Boolean}
     */
    @PostMapping("/api/v1/banner/updateBanner/{id}")
    RestResult updateBanner(@PathVariable("id") Long id, @RequestBody BannerInfoReq req);


    /**
     * 删除 Banner
     *
     * @param id
     * @return {@link Boolean}
     */
    @DeleteMapping("/api/v1/banner/{id}")
    RestResult deleteBanner(@PathVariable("id") Long id);


    /**
     * 禁用 Banner
     *
     * @param id
     * @return {@link Boolean}
     */
    @PostMapping("/api/v1/banner/disable/{id}")
    RestResult disableBanner(@PathVariable("id") Long id);


    /**
     * 启用 Banner
     *
     * @param id
     * @return {@link Boolean}
     */
    @PostMapping("/api/v1/banner/enable/{id}")
    RestResult enableBanner(@PathVariable("id") Long id);


    /**
     * 分页查询 Banner列表
     *
     * @param pageRequest
     * @return {@link RestResult}<{@link PageReturn}<{@link BannerInfoListPageDTO}>>
     */
    @PostMapping("/api/v1/banner/listBanners")
    RestResult<PageReturn<BannerInfoListPageDTO>> listBanners(@RequestBody(required = false) PageRequest pageRequest);
}
