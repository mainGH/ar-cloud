package org.ar.wallet.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageRequest;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.AnnouncementDTO;
import org.ar.common.pay.dto.AnnouncementLinkDTO;
import org.ar.common.pay.dto.AnnouncementListPageDTO;
import org.ar.common.pay.req.AnnouncementInfoReq;
import org.ar.common.web.utils.UserContext;
import org.ar.wallet.entity.Announcement;
import org.ar.wallet.mapper.AnnouncementMapper;
import org.ar.wallet.property.ArProperty;
import org.ar.wallet.service.IAnnouncementService;
import org.ar.wallet.vo.AnnouncementVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Banner信息表 服务实现类
 * </p>
 *
 * @author
 * @since 2024-02-29
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements IAnnouncementService {


    private final ArProperty arProperty;

    /**
     * 前台-获取公告列表
     *
     * @return {@link RestResult}<{@link List}<{@link AnnouncementVo}>>
     */
    @Override
    public RestResult<PageReturn<AnnouncementVo>> getAnnouncementList(PageRequest req) {

        if (req == null) {
            req = new PageRequest();
        }

        Page<Announcement> pageAnnouncement = new Page<>();
        pageAnnouncement.setCurrent(req.getPageNo());
        pageAnnouncement.setSize(req.getPageSize());

        LambdaQueryChainWrapper<Announcement> lambdaQuery = lambdaQuery();

        lambdaQuery.eq(Announcement::getStatus, 1);

        //获取未删除的条目 并根据 序号进行排序 (数字小排前面)
        lambdaQuery.eq(Announcement::getDeleted, 0).orderByAsc(Announcement::getSortOrder);

        baseMapper.selectPage(pageAnnouncement, lambdaQuery.getWrapper());

        List<Announcement> records = pageAnnouncement.getRecords();

        PageReturn<Announcement> flush = PageUtils.flush(pageAnnouncement, records);

        //IPage＜实体＞转 IPage＜Vo＞
        ArrayList<AnnouncementVo> announcementVoList = new ArrayList<>();

        for (Announcement Announcement : flush.getList()) {

            AnnouncementVo announcementVo = new AnnouncementVo();

            BeanUtil.copyProperties(Announcement, announcementVo);

            announcementVoList.add(announcementVo);
        }

        PageReturn<AnnouncementVo> announcementVoListPageReturn = new PageReturn<>();
        announcementVoListPageReturn.setPageNo(flush.getPageNo());
        announcementVoListPageReturn.setPageSize(flush.getPageSize());
        announcementVoListPageReturn.setTotal(flush.getTotal());
        announcementVoListPageReturn.setList(announcementVoList);

        return RestResult.ok(announcementVoListPageReturn);
    }

    /**
     * 新增 公告
     *
     * @param req
     * @return boolean
     */
    @Override
    public RestResult createAnnouncement(AnnouncementInfoReq req) {

        // 检查是否存在相同的排序值
        int count = lambdaQuery()
                .eq(Announcement::getSortOrder, req.getSortOrder())
                .eq(Announcement::getDeleted, 0)
                .count();
        if (count > 0) {
            //排序值重复
            return RestResult.failure(ResultCode.SORT_ORDER_DUPLICATED);
        }

        Announcement announcement = new Announcement();

        BeanUtils.copyProperties(req, announcement);

        // 设置bannerInfo的属性
        if (save(announcement)) {
            return RestResult.ok();
        }

        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }

    /**
     * 根据id获取公告信息
     *
     * @param id
     * @return {@link RestResult}<{@link AnnouncementDTO}>
     */
    @Override
    public RestResult<AnnouncementDTO> getAnnouncementById(Long id) {

        Announcement announcement = lambdaQuery()
                .eq(Announcement::getId, id)
                .eq(Announcement::getDeleted, 0)
                .one();

        if (announcement != null) {
            AnnouncementDTO announcementDTO = new AnnouncementDTO();
            BeanUtils.copyProperties(announcement, announcementDTO);

            return RestResult.ok(announcementDTO);
        } else {
            return RestResult.failure(ResultCode.DATA_NOT_FOUND);
        }
    }

    /**
     * 更新 公告信息
     *
     * @param id
     * @param req
     * @return {@link RestResult}
     */
    @Override
    public RestResult updateAnnouncement(Long id, AnnouncementInfoReq req) {

        // 检查是否存在相同的排序值且不是当前正在更新的banner
        int count = lambdaQuery()
                .eq(Announcement::getSortOrder, req.getSortOrder())
                .ne(Announcement::getId, id) // 排除当前正在更新的banner
                .eq(Announcement::getDeleted, 0)
                .count();
        if (count > 0) {
            //排序值重复
            return RestResult.failure(ResultCode.SORT_ORDER_DUPLICATED);
        }

        Announcement announcement = getById(id);
        if (announcement != null) {
            // 修改bannerInfo的属性
            announcement.setAnnouncementTitle(req.getAnnouncementTitle());
            announcement.setAnnouncementContent(req.getAnnouncementContent());
            announcement.setSortOrder(req.getSortOrder());
            announcement.setStatus(req.getStatus());
            boolean update = updateById(announcement);

            return update ? RestResult.ok() : RestResult.failed();
        } else {
            return RestResult.failure(ResultCode.DATA_NOT_FOUND);
        }
    }


    /**
     * 删除公告
     *
     * @param id
     * @return boolean
     */
    @Override
    public boolean deleteAnnouncement(Long id) {
        return lambdaUpdate().eq(Announcement::getId, id).set(Announcement::getDeleted, 1).update();
    }


    /**
     * 禁用公告
     *
     * @param id
     * @return boolean
     */
    @Override
    public boolean disableAnnouncement(Long id) {

        Announcement announcement = getById(id);
        if (announcement != null) {
            announcement.setStatus(0); // 0为禁用状态
            return updateById(announcement);
        }
        return false;
    }


    /**
     * 启用公告
     *
     * @param id
     * @return boolean
     */
    @Override
    public boolean enableAnnouncement(Long id) {

        Announcement announcement = getById(id);
        if (announcement != null) {
            announcement.setStatus(1); // 1为启用状态
            return updateById(announcement);
        }
        return false;
    }

    /**
     * 分页查询 公告列表
     *
     * @param req
     * @return {@link RestResult}<{@link PageReturn}<{@link AnnouncementListPageDTO}>>
     */
    @Override
    public RestResult<PageReturn<AnnouncementListPageDTO>> listAnnouncements(PageRequest req) {


        //获取当前用户id
        Long currentUserId = UserContext.getCurrentUserId();

        if (currentUserId == null) {
            log.error("分页查询 公告列表失败: 获取当前用户id失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        if (req == null) {
            req = new PageRequest();
        }

        Page<Announcement> pageAnnouncement = new Page<>();
        pageAnnouncement.setCurrent(req.getPageNo());
        pageAnnouncement.setSize(req.getPageSize());

        LambdaQueryChainWrapper<Announcement> lambdaQuery = lambdaQuery();


        //获取未删除的条目 并根据 序号进行排序 (数字小排前面)
        lambdaQuery.eq(Announcement::getDeleted, 0).orderByAsc(Announcement::getSortOrder);

        baseMapper.selectPage(pageAnnouncement, lambdaQuery.getWrapper());

        List<Announcement> records = pageAnnouncement.getRecords();

        PageReturn<Announcement> flush = PageUtils.flush(pageAnnouncement, records);

        //IPage＜实体＞转 IPage＜Vo＞
        ArrayList<AnnouncementListPageDTO> AnnouncementListPageDTOList = new ArrayList<>();

        for (Announcement Announcement : flush.getList()) {

            AnnouncementListPageDTO announcementListPageDTO = new AnnouncementListPageDTO();


            BeanUtil.copyProperties(Announcement, announcementListPageDTO);

            //最后更新时间
            if (announcementListPageDTO.getUpdateTime() == null) {
                announcementListPageDTO.setUpdateTime(announcementListPageDTO.getCreateTime());
            }

            //操作人
            if (StringUtils.isEmpty(announcementListPageDTO.getUpdateBy())) {
                announcementListPageDTO.setUpdateBy(Announcement.getCreateBy());
            }
            AnnouncementListPageDTOList.add(announcementListPageDTO);
        }

        PageReturn<AnnouncementListPageDTO> announcementListPageDTOPageReturn = new PageReturn<>();
        announcementListPageDTOPageReturn.setPageNo(flush.getPageNo());
        announcementListPageDTOPageReturn.setPageSize(flush.getPageSize());
        announcementListPageDTOPageReturn.setTotal(flush.getTotal());
        announcementListPageDTOPageReturn.setList(AnnouncementListPageDTOList);

        log.info("分页查询 公告列表成功: 用户id: {}, req: {}, 返回数据: {}", currentUserId, req, announcementListPageDTOPageReturn);

        return RestResult.ok(announcementListPageDTOPageReturn);
    }


    /**
     * 前台-获取公告详情页
     *
     * @param id
     * @return {@link RestResult}<{@link AnnouncementVo}>
     */
    @Override
    public RestResult<AnnouncementVo> findAnnouncementDetail(Long id) {
        Announcement announcement = lambdaQuery()
                .eq(Announcement::getId, id)
                .eq(Announcement::getStatus, 1)
                .eq(Announcement::getDeleted, 0)
                .one();

        if (announcement != null) {
            AnnouncementVo announcementVo = new AnnouncementVo();
            BeanUtils.copyProperties(announcement, announcementVo);

            return RestResult.ok(announcementVo);
        } else {
            return RestResult.failure(ResultCode.DATA_NOT_FOUND);
        }
    }

    /**
     * 获取公告链接
     *
     * @param id
     * @return {@link RestResult}<{@link AnnouncementLinkDTO}>
     */
    @Override
    public RestResult<AnnouncementLinkDTO> fetchAnnouncementLinkById(Long id) {

        AnnouncementLinkDTO announcementLinkDTO = new AnnouncementLinkDTO();
        announcementLinkDTO.setAnnouncementLink(arProperty.getAnnouncementLink() + id);
        return RestResult.ok(announcementLinkDTO);
    }
}
