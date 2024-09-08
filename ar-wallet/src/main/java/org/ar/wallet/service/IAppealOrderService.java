package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.vertx.core.json.JsonObject;
import org.ar.common.core.page.PageRequestHome;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.AppealOrderDTO;
import org.ar.common.pay.dto.AppealOrderExportDTO;
import org.ar.common.pay.req.AppealOrderIdReq;
import org.ar.common.pay.req.AppealOrderPageListReq;
import org.ar.wallet.entity.AppealOrder;
import org.ar.wallet.req.PlatformOrderReq;
import org.ar.wallet.vo.AppealDetailsVo;
import org.ar.wallet.vo.AppealOrderVo;
import org.ar.wallet.vo.ViewMyAppealVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;

/**
 * @author
 */
public interface IAppealOrderService extends IService<AppealOrder> {

    Boolean submitAppeal(MultipartFile[] file,
                         MultipartFile videoUpload,
                         Integer appealType,
                         String orderNo,
                         String reason,
                         String mid,
                         String mAccount,
                         BigDecimal orderAmount,
                         String belongMerchantCode) throws FileNotFoundException;

    AppealOrderVo queryAppealOrder(String orderNo, Integer appealType) throws Exception;



    AppealOrderDTO pay(AppealOrderIdReq req);

    AppealOrderDTO nopay(AppealOrderIdReq req);



    PageReturn<AppealOrderDTO> listPage(AppealOrderPageListReq req) throws ExecutionException, InterruptedException;
    PageReturn<AppealOrderExportDTO> listPageExport(AppealOrderPageListReq req) throws ExecutionException, InterruptedException;


    /**
     * 申诉-文件处理
     *
     * @param images
     * @param video
     * @return {@link JsonObject}
     */
    JsonObject saveFile(MultipartFile[] images, MultipartFile video);

    /**
     * 根据买入订单号获取申诉订单
     *
     * @param platformOrder
     */
    AppealOrder getAppealOrderByBuyOrderNo(String platformOrder);


    /**
     * 根据卖出订单号获取申诉订单
     *
     * @param platformOrder
     */
    AppealOrder getAppealOrderBySellOrderNo(String platformOrder);

    /**
     * 查看订单申诉详情
     *
     * @param platformOrderReq
     * @return {@link RestResult}<{@link AppealDetailsVo}>
     */
    RestResult<AppealDetailsVo> viewAppealDetails(PlatformOrderReq platformOrderReq, String type);

    /**
     * 我的申诉
     *
     * @param pageRequestHome
     * @return {@link RestResult}<{@link PageReturn}<{@link ViewMyAppealVo}>>
     */
    RestResult<PageReturn<ViewMyAppealVo>> viewMyAppeal(PageRequestHome pageRequestHome);

    /**
     * 变更会员信用分
     *
     * @param orderSuccess
     * @param buyerId
     * @param sellerId
     * @param appealOrder
     */
    void changeCreditScore(boolean orderSuccess, String buyerId, String sellerId, AppealOrder appealOrder);
}
