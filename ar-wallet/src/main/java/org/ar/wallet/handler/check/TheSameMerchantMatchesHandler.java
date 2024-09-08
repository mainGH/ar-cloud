//package org.ar.wallet.handler.check;
//
//import org.ar.common.core.annotation.HandlerAnnotation;
//
//import org.ar.wallet.entity.CollectionOrder;
//import org.ar.wallet.entity.MatchingOrder;
//import org.ar.wallet.entity.PaymentOrder;
//import org.ar.wallet.handler.Handler;
//import org.ar.wallet.util.MatchesUtil;
//
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collector;
//import java.util.stream.Collectors;
//
//@HandlerAnnotation(offset = 2)
//public class TheSameMerchantMatchesHandler extends Handler {
//
//    private Handler handler;
//    @Override
//    public void setNextHandler(Handler handler){
//        this.handler = handler;
//    }
//    @Override
//    public List<MatchingOrder> handler(List<CollectionOrder> clist, PaymentOrder paymentOrder){
//        Map<String,List<CollectionOrder>> map  =  clist.stream().collect(Collectors.groupingBy(CollectionOrder::getMerchantCode));
//        List<CollectionOrder>  plist = map.get(paymentOrder.getMerchantCode());
//        List<MatchingOrder> rlist = MatchesUtil.getMatchedOrder(plist,paymentOrder);
//        if(rlist!=null&&rlist.size()>0) return rlist;
//        return this.handler.handler(clist,paymentOrder);
//    }
//}
