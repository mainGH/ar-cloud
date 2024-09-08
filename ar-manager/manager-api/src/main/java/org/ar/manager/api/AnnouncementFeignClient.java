package org.ar.manager.api;

import org.ar.common.core.page.PageRequest;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.AnnouncementDTO;
import org.ar.common.pay.dto.AnnouncementLinkDTO;
import org.ar.common.pay.dto.AnnouncementListPageDTO;
import org.ar.common.pay.req.AnnouncementInfoReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * @author Admin
 */
@FeignClient(value = "ar-wallet", contextId = "announcement")
public interface AnnouncementFeignClient {


    /**
     * 新增公告
     *
     * @param req
     * @return {@link RestResult}
     */
    @PostMapping("/api/v1/announcement/createAnnouncement")
    RestResult createAnnouncement(@RequestBody AnnouncementInfoReq req);

    /**
     * 根据id获取公告信息
     *
     * @param id
     * @return {@link RestResult}<{@link AnnouncementDTO}>
     */
    @GetMapping("/api/v1/announcement/getAnnouncementById/{id}")
    RestResult<AnnouncementDTO> getAnnouncementById(@PathVariable("id") Long id);

    /**
     * 更新公告信息
     *
     * @param id
     * @param req
     * @return {@link RestResult}
     */
    @PostMapping("/api/v1/announcement/updateAnnouncement/{id}")
    RestResult updateAnnouncement(@PathVariable("id") Long id, @RequestBody AnnouncementInfoReq req);

    /**
     * 删除公告信息
     *
     * @param id
     * @return {@link RestResult}
     */
    @DeleteMapping("/api/v1/announcement/{id}")
    RestResult deleteAnnouncement(@PathVariable("id") Long id);

    /**
     * 禁用公告
     *
     * @param id
     * @return {@link RestResult}
     */
    @PostMapping("/api/v1/announcement/disable/{id}")
    RestResult disableAnnouncement(@PathVariable("id") Long id);

    /**
     * 启用公告
     *
     * @param id
     * @return {@link RestResult}
     */
    @PostMapping("/api/v1/announcement/enable/{id}")
    RestResult enableAnnouncement(@PathVariable("id") Long id);

    /**
     * 分页查询公告列表
     *
     * @param pageRequest
     * @return {@link RestResult}<{@link PageReturn}<{@link AnnouncementListPageDTO}>>
     */
    @PostMapping("/api/v1/announcement/listAnnouncements")
    RestResult<PageReturn<AnnouncementListPageDTO>> listAnnouncements(@RequestBody(required = false) PageRequest pageRequest);

    /**
     * 获取公告链接
     *
     * @param id
     * @return {@link RestResult}<{@link AnnouncementLinkDTO}>
     */
    @GetMapping("/api/v1/announcement/fetchAnnouncementLinkById/{id}")
    RestResult<AnnouncementLinkDTO> fetchAnnouncementLinkById(@PathVariable("id") Long id);
}
