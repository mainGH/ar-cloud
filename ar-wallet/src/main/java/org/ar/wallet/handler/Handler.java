package org.ar.wallet.handler;


import org.ar.wallet.entity.CollectionOrder;
import org.ar.wallet.entity.MatchingOrder;
import org.ar.wallet.entity.PaymentOrder;

import java.util.List;

public abstract class Handler {

    public abstract void setNextHandler(Handler handler);

    /*
     * 使用责任链模式匹配订单(用代付订单去匹配充值订单)
     * */
    public abstract List<MatchingOrder> handler(List<CollectionOrder> clist, PaymentOrder paymentOrder);
}
