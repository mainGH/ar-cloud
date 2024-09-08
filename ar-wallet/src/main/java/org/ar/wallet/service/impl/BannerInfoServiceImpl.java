package org.ar.wallet.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageRequest;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.BannerInfoDTO;
import org.ar.common.pay.dto.BannerInfoListPageDTO;
import org.ar.common.pay.req.BannerInfoReq;
import org.ar.common.web.utils.UserContext;
import org.ar.wallet.entity.BannerInfo;
import org.ar.wallet.mapper.BannerInfoMapper;
import org.ar.wallet.service.IBannerInfoService;
import org.ar.wallet.vo.BannerListVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * Banner信息表 服务实现类
 * </p>
 *
 * @author
 * @since 2024-02-28
 */
@Service
@Slf4j
public class BannerInfoServiceImpl extends ServiceImpl<BannerInfoMapper, BannerInfo> implements IBannerInfoService {

    @Value("${oss.baseUrl}")
    private String baseUrl;

    /**
     * 新增 Banner
     *
     * @param req
     * @return boolean
     */
    @Override
    public RestResult createBanner(BannerInfoReq req) {

        // 检查是否存在相同的排序值
        int count = lambdaQuery()
                .eq(BannerInfo::getSortOrder, req.getSortOrder())
                .eq(BannerInfo::getDeleted, 0)
                .count();
        if (count > 0) {
            //排序值重复
            return RestResult.failure(ResultCode.SORT_ORDER_DUPLICATED);
        }

        BannerInfo bannerInfo = new BannerInfo(); // 转换req到entity

        BeanUtils.copyProperties(req, bannerInfo);

        //拼接图片链接
        bannerInfo.setBannerImageUrl(baseUrl + bannerInfo.getBannerImageUrl());

        // 设置bannerInfo的属性
        if (save(bannerInfo)){
            return RestResult.ok();
        }

        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }


    /**
     * 根据ID查询Banner信息
     *
     * @param id
     * @return {@link BannerInfo}
     */
    @Override
    public RestResult<BannerInfoDTO> getBannerById(Long id) {

        BannerInfo bannerInfo = lambdaQuery()
                .eq(BannerInfo::getId, id)
                .eq(BannerInfo::getDeleted, 0)
                .one();

        if (bannerInfo != null) {
            BannerInfoDTO bannerInfoDTO = new BannerInfoDTO();
            BeanUtils.copyProperties(bannerInfo, bannerInfoDTO);

            return RestResult.ok(bannerInfoDTO);
        } else {
            return RestResult.failure(ResultCode.DATA_NOT_FOUND);
        }
    }

    /**
     * 更新 Banner
     *
     * @param id
     * @param req
     * @return boolean
     */
    @Override
    public RestResult updateBanner(Long id, BannerInfoReq req) {

        // 检查是否存在相同的排序值且不是当前正在更新的banner
        int count = lambdaQuery()
                .eq(BannerInfo::getSortOrder, req.getSortOrder())
                .ne(BannerInfo::getId, id) // 排除当前正在更新的banner
                .eq(BannerInfo::getDeleted, 0)
                .count();
        if (count > 0) {
            //排序值重复
            return RestResult.failure(ResultCode.SORT_ORDER_DUPLICATED);
        }

        // 检查bannerImageUrl是否以"http"开头
        String bannerImageUrl = req.getBannerImageUrl();


        if (bannerImageUrl != null && !bannerImageUrl.startsWith("https://")) {
            // 如果不是以"http"开头，则进行拼接
            bannerImageUrl = baseUrl + bannerImageUrl;
        }

        BannerInfo bannerInfo = getById(id);
        if (bannerInfo != null) {
            // 修改bannerInfo的属性
            bannerInfo.setBannerType(req.getBannerType());
            bannerInfo.setSortOrder(req.getSortOrder());
            bannerInfo.setRedirectUrl(req.getRedirectUrl());
            bannerInfo.setBannerImageUrl(bannerImageUrl);
            bannerInfo.setStatus(req.getStatus());
            bannerInfo.setLinkType(req.getLinkType());
            boolean update = updateById(bannerInfo);

            return update ? RestResult.ok() : RestResult.failed();
        } else {
            return RestResult.failure(ResultCode.DATA_NOT_FOUND);
        }
    }

    /**
     * 删除 Banner
     *
     * @param id
     * @return boolean
     */
    @Override
    public boolean deleteBanner(Long id) {
        return lambdaUpdate().eq(BannerInfo::getId, id).set(BannerInfo::getDeleted, 1).update();
    }

    /**
     * 禁用 Banner
     *
     * @param id
     * @return boolean
     */
    @Override
    public boolean disableBanner(Long id) {

        BannerInfo bannerInfo = getById(id);
        if (bannerInfo != null) {
            bannerInfo.setStatus(0); // 0为禁用状态
            return updateById(bannerInfo);
        }
        return false;
    }


    /**
     * 启用 Banner
     *
     * @param id
     * @return boolean
     */
    @Override
    public boolean enableBanner(Long id) {
        BannerInfo bannerInfo = getById(id);
        if (bannerInfo != null) {
            bannerInfo.setStatus(1); // 1为启用状态
            return updateById(bannerInfo);
        }
        return false;
    }


    /**
     * 分页查询 banner列表
     *
     * @param req
     * @return {@link RestResult}<{@link PageReturn}<{@link BannerInfoListPageDTO}>>
     */
    @Override
    public RestResult<PageReturn<BannerInfoListPageDTO>> listPage(PageRequest req) {

        //获取当前用户id
        Long currentUserId = UserContext.getCurrentUserId();

        if (currentUserId == null) {
            log.error("分页查询 banner列表失败: 获取当前用户id失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        if (req == null) {
            req = new PageRequest();
        }

        Page<BannerInfo> pageBannerInfo = new Page<>();
        pageBannerInfo.setCurrent(req.getPageNo());
        pageBannerInfo.setSize(req.getPageSize());

        LambdaQueryChainWrapper<BannerInfo> lambdaQuery = lambdaQuery();


        //获取未删除的条目 并根据 序号进行排序 (数字小排前面)
        lambdaQuery.eq(BannerInfo::getDeleted, 0).orderByAsc(BannerInfo::getSortOrder);

        baseMapper.selectPage(pageBannerInfo, lambdaQuery.getWrapper());

        List<BannerInfo> records = pageBannerInfo.getRecords();

        PageReturn<BannerInfo> flush = PageUtils.flush(pageBannerInfo, records);

        //IPage＜实体＞转 IPage＜Vo＞
        ArrayList<BannerInfoListPageDTO> bannerInfoListPageDTOList = new ArrayList<>();

        for (BannerInfo bannerInfo : flush.getList()) {

            BannerInfoListPageDTO bannerInfoListPageDTO = new BannerInfoListPageDTO();

            BeanUtil.copyProperties(bannerInfo, bannerInfoListPageDTO);

            //最后更新时间
            if (bannerInfoListPageDTO.getUpdateTime() == null) {
                bannerInfoListPageDTO.setUpdateTime(bannerInfo.getCreateTime());
            }

            //操作人
            if (StringUtils.isEmpty(bannerInfoListPageDTO.getUpdateBy())) {
                bannerInfoListPageDTO.setUpdateBy(bannerInfo.getCreateBy());
            }
            bannerInfoListPageDTOList.add(bannerInfoListPageDTO);
        }

        PageReturn<BannerInfoListPageDTO> bannerInfoListPageDTOPageReturn = new PageReturn<>();
        bannerInfoListPageDTOPageReturn.setPageNo(flush.getPageNo());
        bannerInfoListPageDTOPageReturn.setPageSize(flush.getPageSize());
        bannerInfoListPageDTOPageReturn.setTotal(flush.getTotal());
        bannerInfoListPageDTOPageReturn.setList(bannerInfoListPageDTOList);

        log.info("分页查询 banner列表成功: 用户id: {}, req: {}, 返回数据: {}", currentUserId, req, bannerInfoListPageDTOPageReturn);

        return RestResult.ok(bannerInfoListPageDTOPageReturn);
    }


    /**
     * 获取 Banner列表
     *
     * @return {@link RestResult}<{@link BannerListVo}>
     */
    @Override
    public RestResult<Map<String, List<BannerListVo>>> getBannerList() {
        List<BannerInfo> list = lambdaQuery()
                .eq(BannerInfo::getDeleted, 0)
                .eq(BannerInfo::getStatus, 1)
                .orderByAsc(BannerInfo::getSortOrder) // 添加排序
                .list();

        Map<String, List<BannerListVo>> groupedBanners = list.stream()
                .map(bannerInfo -> {
                    BannerListVo bannerListVo = new BannerListVo();
                    BeanUtils.copyProperties(bannerInfo, bannerListVo);
                    return bannerListVo;
                })
                .collect(Collectors.groupingBy(bannerListVo -> {
                    // 根据bannerType的值转换为英文标识符
                    switch (bannerListVo.getBannerType()) {
                        case "01":
                            return "bannerOne";
                        case "02":
                            return "bannerTwo";
                        default:
                            return "unknownType"; // 默认分组
                    }
                }));

        return RestResult.ok(groupedBanners);
    }
}
