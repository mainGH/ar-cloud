package org.ar.pay.handler;

import org.ar.pay.entity.BankInfo;
import org.ar.pay.entity.PayConfig;
import org.ar.pay.vo.PaymentOrderVo;

import java.util.List;
import java.util.Map;

public abstract  class Handler {

    public abstract  void setNextHandler(Handler handler);
    public abstract  PayConfig handler(PaymentOrderVo paymentOrderVo,PayConfig config, Map<String, List<BankInfo>> listMap);
}
