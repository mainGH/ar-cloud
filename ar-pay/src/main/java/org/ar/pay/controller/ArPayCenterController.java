package org.ar.pay.controller;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.pay.entity.CollectionOrder;
import org.ar.pay.entity.MerchantInfo;
import org.ar.pay.entity.PayConfig;
import org.ar.pay.req.PayConfigReq;
import org.ar.pay.service.*;
import org.ar.pay.util.IpUtil;
import org.ar.pay.util.SignAPI;
import org.ar.pay.util.SignUtil;
import org.ar.pay.util.SpringContextUtil;
import org.ar.pay.vo.CollectionOrderVo;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/payCenter")
@Api(description = "支付下单控制器")
public class ArPayCenterController {
    private final IMerchantInfoService merchantInfoService;
    private final IPayConfigService payConfigService;
    private final IPaymentOrderService paymentOrderService;
    private final ICollectionOrderService collectionOrderService;

    @PostMapping("/pay")
    @ApiOperation(value = "支付下单接口")
    public RestResult<JSONObject> pay(@RequestBody @ApiParam CollectionOrderVo collectionOrderVo, HttpServletRequest request) {

        log.info("支付下单接口collectionOrderVo: {}", collectionOrderVo);

        //查询该笔订单号是否已存在 (订单号去重)
        QueryWrapper<CollectionOrder> collectionOrderQueryWrapper = new QueryWrapper<>();
        collectionOrderQueryWrapper.eq("merchant_order", collectionOrderVo.getMerchantOrder());
        if (collectionOrderService.count(collectionOrderQueryWrapper) > 0) {
            JSONObject errorJson = new JSONObject();
            //业务状态码
            errorJson.put("code", ResultCode.SYSTEM_EXECUTION_ERROR.getCode());
            //错误信息
            errorJson.put("errorMsg", "订单号重复");
            log.info("支付下单接口: {}, 订单号重复", collectionOrderVo);
            return RestResult.ok(errorJson);
        }

        PayConfigReq req = new PayConfigReq();
        //国家
        req.setCountry(collectionOrderVo.getCountry());

        //支付方式
        req.setPayType(collectionOrderVo.getPayType());

        //币种
        req.setCurrency(collectionOrderVo.getCurrency());

        //商户号
        req.setMerchantCode(collectionOrderVo.getMerchantCode());

        //状态
        req.setStatus("1");

        log.info("支付下单接口req: {}", req);


        JSONObject json = null;


        //判断是否存在该商户
        MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(collectionOrderVo.getMerchantCode());
        if (merchantInfo == null || StringUtils.isEmpty(merchantInfo.getCode())) {
            JSONObject errorJson = new JSONObject();
            //业务状态码
            errorJson.put("code", ResultCode.SYSTEM_EXECUTION_ERROR.getCode());
            //错误信息
            errorJson.put("errorMsg", "商户号不存在");
            log.info("支付下单接口: {}, 商户号不存在", merchantInfo);
            return RestResult.ok(errorJson);
        }

        //md5验签
        String signInfo = SignUtil.sortObject(collectionOrderVo);

        log.info("支付下单接口 签名串: {}, ", signInfo);

        String sign = SignAPI.sign(signInfo, merchantInfo.getMd5Key());

        if (!sign.equals(collectionOrderVo.getSign())) {
            JSONObject errorJson = new JSONObject();
            //业务状态码
            errorJson.put("code", ResultCode.SYSTEM_EXECUTION_ERROR.getCode());
            //错误信息
            errorJson.put("errorMsg", "签名错误");

            log.info("支付下单接口: {}, 签名串: {}, 签名错误: {}", collectionOrderVo, signInfo, sign);
            return RestResult.ok(errorJson);
        }

        //设置平台订单号
        collectionOrderVo.setPlatformOrder("AR" + collectionOrderVo.getMerchantOrder());

        //根据条件过滤查询到符合的三方支付
        List<PayConfig> list = payConfigService.getPayConfigByCondtion(req);
        if (list.size() > 0) {
            //选择一个三方支付
            PayConfig payConfig = (PayConfig) list.get(0);
            collectionOrderVo.setThirdCode(payConfig.getThirdCode());
            CollectionOrder collectionOrder = new CollectionOrder();
            BeanUtils.copyProperties(collectionOrderVo, collectionOrder);

            //获取请求IP
            String clinetIpByReq = IpUtil.getClinetIpByReq(request);
            //将订单详情数据和请求IP记录到log_info字段
            JSONObject logInfo = new JSONObject();
            logInfo.put("req_data", collectionOrder);
            logInfo.put("req_ip", clinetIpByReq);
            collectionOrder.setLogInfo(JSONObject.toJSONString(logInfo));

            log.info("支付下单接口平台订单号: {}, 下单数据: {}", collectionOrderVo.getPlatformOrder(), logInfo);

            //获取到三方支付的实现类
            PayRouteAbstract route = (PayRouteAbstract) SpringContextUtil.getBean(payConfig.getServiceName());

            //三方支付实现类调用三方下单接口
            json = route.rountePayByParameter(payConfig, collectionOrder);

            //设置三方订单号
            collectionOrder.setThirdOrder(json.getString("thirdOrder"));
            //删除三方订单号(不对商户显示)
            json.remove("thirdOrder");

            //设置订单费率
            BigDecimal payRate = new BigDecimal(String.valueOf(merchantInfoService.getRateByCode(collectionOrder.getMerchantCode()).get("pay_rate")));
            collectionOrder.setOrderRate((payRate));

            //设置费用 订单金额 * 费率)
            collectionOrder.setCost(collectionOrder.getAmount().multiply((payRate.divide(BigDecimal.valueOf(100)))));

            //将支付订单写入数据库
            collectionOrderService.save(collectionOrder);

        } else {
            return RestResult.failed("未匹配到通道");
        }
        //将支付下单接口数据返回给商户
        return RestResult.ok(json);
    }

}


