package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageRequestHome;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.CollectionInfoDTO;
import org.ar.common.pay.req.CollectionInfoIdReq;
import org.ar.common.pay.req.CollectionInfoListPageReq;
import org.ar.wallet.entity.CollectionInfo;
import org.ar.wallet.entity.MemberInfo;
import org.ar.wallet.req.CheckUpiIdDuplicateReq;
import org.ar.wallet.req.CollectioninfoIdReq;
import org.ar.wallet.req.FrontendCollectionInfoReq;
import org.ar.wallet.req.SellReq;
import org.ar.wallet.vo.CheckUpiIdDuplicateVo;
import org.ar.wallet.vo.CollectionInfoVo;
import org.ar.wallet.vo.NormalCollectionInfoVo;

import java.util.List;

/**
 * @author
 */
public interface ICollectionInfoService extends IService<CollectionInfo> {


    /**
     * 开启收款时校验
     *
     * @param collectionInfo
     * @return {@link RestResult}
     */
    RestResult enableCollectionVerification(CollectionInfo collectionInfo, MemberInfo memberInfo);


    /**
     * 停止收款时校验
     *
     * @param collectionInfo
     * @return {@link RestResult}
     */
    RestResult stopCollectionVerification(CollectionInfo collectionInfo, MemberInfo memberInfo);


    /**
     * @param pageRequestHome
     * @return {@link RestResult}<{@link PageReturn}<{@link CollectionInfoVo}>>
     *//*
     * 获取当前用户收款信息
     * */
    RestResult<PageReturn<CollectionInfoVo>> currentCollectionInfo(PageRequestHome pageRequestHome);


    /**
     * 获取当前用户在正常收款的收款信息
     *
     * @param pageRequestHome
     * @return {@link RestResult}<{@link PageReturn}<{@link NormalCollectionInfoVo}>>
     */
    RestResult<PageReturn<NormalCollectionInfoVo>> currentNormalCollectionInfo(PageRequestHome pageRequestHome);


    /**
     * 更新收款信息: 今日收款金额、今日收款笔数
     *
     * @param sellReq
     * @return {@link Boolean}
     */
    Boolean addCollectionInfoQuotaAndCount(SellReq sellReq, CollectionInfo collectionInfo);

    /**
     * 删除收款信息
     *
     * @param collectionInfoId
     * @return {@link Boolean}
     */
    Boolean deleteCollectionInfo(Long collectionInfoId);

    PageReturn<CollectionInfoDTO> listPage(CollectionInfoListPageReq req);

    List<CollectionInfoDTO> getListByUid(CollectionInfoIdReq req);


    /**
     * 开启收款处理
     *
     * @param collectioninfoIdReq
     * @return {@link RestResult}
     */
    RestResult enableCollectionProcessing(CollectioninfoIdReq collectioninfoIdReq);


    /**
     * 停止收款处理
     *
     * @param collectioninfoIdReq
     * @return {@link RestResult}
     */
    RestResult stopCollectionProcessing(CollectioninfoIdReq collectioninfoIdReq);


    /**
     * 删除收款处理
     *
     * @param collectionInfoId
     * @return {@link RestResult}
     */
    RestResult deleteCollectionInfoProcessing(Long collectionInfoId);


    /**
     * 添加收款信息处理
     *
     * @param frontendCollectionInfoReq
     * @return {@link RestResult}
     */
    RestResult createcollectionInfoProcessing(FrontendCollectionInfoReq frontendCollectionInfoReq);


    /**
     * 根据upi_id和upi_name获取收款信息
     *
     * @param upiId
     * @param upiName
     * @return {@link RestResult}
     */
    CollectionInfo getPaymentDetailsByUpiIdAndUpiName(String upiId, String upiName);


    /**
     * 查询 upi_id 是否存在
     *
     * @param upiId
     * @return {@link CollectionInfo}
     */
    CollectionInfo getPaymentDetailsByUpiId(String upiId);

    /**
     * 设置默认收款信息
     *
     * @param collectioninfoIdReq
     * @return {@link RestResult}
     */
    RestResult setDefaultCollectionInfoReq(CollectioninfoIdReq collectioninfoIdReq);


    /**
     * 校验UIPI_ID是否重复
     *
     * @param checkUpiIdDuplicateReq
     * @return {@link RestResult}
     */
    RestResult<CheckUpiIdDuplicateVo> checkUpiIdDuplicate(CheckUpiIdDuplicateReq checkUpiIdDuplicateReq);


    /**
     * 获取会员默认收款信息
     *
     * @param memberId
     * @return {@link CollectionInfo}
     */
    CollectionInfo getDefaultCollectionInfoByMemberId(String memberId);


    /**
     * 检查某个会员的收款信息数量是否大于0
     *
     * @param memberId 会员ID
     * @return true 如果数量大于0，否则false
     */
    Boolean hasCollectionInfo(String memberId);
}
