package org.ar.pay.handler.check;

import org.ar.common.core.annotation.HandlerAnnotation;
import org.ar.pay.entity.BankInfo;
import org.ar.pay.entity.PayConfig;
import org.ar.pay.handler.Handler;
import org.ar.pay.vo.PaymentOrderVo;

import java.util.List;
import java.util.Map;

@HandlerAnnotation(offset = 2)
public class AmountRangeCheckHandler extends Handler {

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
        return  this.handler.handler(paymentOrderVo,payConfig,listMap);
    }

}
