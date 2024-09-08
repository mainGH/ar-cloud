//package org.ar.pay.job;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.ar.common.core.constant.RabbitMqConstants;
//import org.ar.pay.entity.CollectionOrder;
//import org.ar.pay.entity.MerchantInfo;
//import org.ar.pay.runable.BarrierRun;
//import org.ar.pay.runable.CallBack;
//import org.ar.pay.runable.CollectionOrderCallback;
//import org.ar.pay.service.ICollectionOrderService;
//import org.ar.pay.service.IMerchantInfoService;
//import org.springframework.amqp.rabbit.connection.CorrelationData;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.stereotype.Component;
//import org.springframework.stereotype.Service;
//import tech.powerjob.worker.core.processor.ProcessResult;
//import tech.powerjob.worker.core.processor.TaskContext;
//import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
//import tech.powerjob.worker.log.OmsLogger;
//import java.util.concurrent.*;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import java.util.stream.Collectors;
//
//@Component("merchantNotify")
//@Slf4j
//@RequiredArgsConstructor
//public class MerchantNotify implements BasicProcessor {
//    private final IMerchantInfoService merchantInfoService;
//    private final ICollectionOrderService collectionOrderService;
//    private final RabbitTemplate rabbitTemplate;
//
//
//    @Override
//    public ProcessResult process(TaskContext context) throws Exception {
//
//
//
//        List<MerchantInfo> list = merchantInfoService.getAllMerchantByStatus();
//        List<CollectionOrder> listCollection =  collectionOrderService.getCollectionOrderBySatus();
//
//
//        Map<String,Object> map = new HashMap<String,Object>();
//        Map<String,List<CollectionOrder>>  mapList = listCollection.stream().collect(Collectors.groupingBy(CollectionOrder::getMerchantCode));
//        for(MerchantInfo merchantInfo : list){
//            List<CollectionOrder> listOrderTemp =  mapList.get(merchantInfo.getCode());
//            if (listOrderTemp != null){
//                for(CollectionOrder collectionOrder : listOrderTemp){
//                    collectionOrder.setKey(merchantInfo.getMd5Key());
//                    rabbitTemplate.convertAndSend(RabbitMqConstants.AR_PAY_QUEUE_NAME,collectionOrder,new CorrelationData(String.valueOf(collectionOrder.getId())));
//                }
//            }
//        }
//
//
//
//
//
//
//
//
//        // 在线日志功能，可以直接在控制台查看任务日志，非常便捷
//        OmsLogger omsLogger = context.getOmsLogger();
//        omsLogger.info("BasicProcessorDemo start to process, current JobParams is {}.", context.getJobParams());
//
//        // TaskContext为任务的上下文信息，包含了在控制台录入的任务元数据，常用字段为
//        // jobParams（任务参数，在控制台录入），instanceParams（任务实例参数，通过 OpenAPI 触发的任务实例才可能存在该参数）
//
//        // 进行实际处理...
//        //System.out.println("11111111");
//
//        // 返回结果，该结果会被持久化到数据库，在前端页面直接查看，极为方便
//        return new ProcessResult(true, "result is xxx");
//    }
//}
