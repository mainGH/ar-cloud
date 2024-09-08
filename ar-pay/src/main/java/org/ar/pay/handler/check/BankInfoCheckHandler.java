package org.ar.pay.handler.check;

import org.ar.common.core.annotation.HandlerAnnotation;
import org.ar.common.core.utils.StringUtils;
import org.ar.pay.entity.BankInfo;
import org.ar.pay.entity.PayConfig;
import org.ar.pay.handler.Handler;
import org.ar.pay.vo.PaymentOrderVo;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@HandlerAnnotation(offset = 1)
public class BankInfoCheckHandler extends Handler {
    private Handler handler;
    @Override
    public void setNextHandler(Handler handler){
        this.handler = handler;
    }
    @Override
    public PayConfig handler(PaymentOrderVo paymentOrderVo,PayConfig payConfig, Map<String, List<BankInfo>> listMap){
        if(payConfig==null){
            return null;
        }

        if("1".equals(payConfig.getBankProvid())){
           List<BankInfo>  list =  listMap.get(payConfig.getThirdCode()+"_"+payConfig.getCountry());
           if(list!=null){
                Map<String,BankInfo> map =  list.stream().collect(Collectors.toMap(BankInfo::getBankCode, Function.identity()));
                if(StringUtils.isEmpty(map.get(paymentOrderVo.getBankCode()))) return null;
           }

        }
       return  this.handler.handler(paymentOrderVo,payConfig,listMap);
    }
}
