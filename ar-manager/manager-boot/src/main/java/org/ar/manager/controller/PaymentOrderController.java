package org.ar.manager.controller;


import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.utils.ExcelUtil;
import org.ar.common.pay.dto.*;
import org.ar.common.pay.req.MatchingOrderReq;
import org.ar.common.pay.req.PaymentOrderGetInfoReq;
import org.ar.common.pay.req.PaymentOrderIdReq;
import org.ar.common.pay.req.PaymentOrderListPageReq;

import org.ar.common.web.utils.UserContext;
import org.ar.manager.annotation.SysLog;
import org.ar.manager.api.PaymentOrderFeignClient;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = {"/api/v1/paymentOrderAdmin", "/paymentOrderAdmin"})
@Api(description = "卖出订单控制器")
public class PaymentOrderController {

    private final PaymentOrderFeignClient paymentOrderFeignClient;



    @PostMapping("/listPage")
    @ApiOperation(value = "卖出订单")
    public RestResult<List<PaymentOrderListPageDTO>> listPage(@RequestBody(required = false) @ApiParam PaymentOrderListPageReq req) {
        RestResult<List<PaymentOrderListPageDTO>> result = paymentOrderFeignClient.listPage(req);
        return result;
    }

    @PostMapping("/export")
    @ApiOperation(value = "卖出订单列表导出")
    public void export(HttpServletResponse response, @RequestBody @ApiParam PaymentOrderListPageReq req) throws IOException {
        req.setPageSize(GlobalConstants.BATCH_SIZE);
        RestResult<List<PaymentOrderExportDTO>> result = paymentOrderFeignClient.listPageExport(req);
        OutputStream outputStream = null;
        BufferedOutputStream bos = null;
        ExcelWriter excelWriter = null;
        Integer exportTotalSize = 0;
        try {
            outputStream = response.getOutputStream();
            exportTotalSize = result.getData().size();
            //必须放到循环外，否则会刷新流
            excelWriter = null;
            List<List<String>> head = null;
            bos = new BufferedOutputStream(outputStream);
            if (req.getLang().equals("zh")) {
                ExcelUtil.setResponseHeader(response, "SellOrdersList");
                excelWriter = EasyExcel.write(bos, PaymentOrderExportDTO.class).build();
                head = ExcelUtil.parseHead(PaymentOrderExportDTO.class);
            } else {
                ExcelUtil.setResponseHeader(response, "SellOrdersList");
                excelWriter = EasyExcel.write(bos, PaymentOrderExportDTO.class).build();
                head = ExcelUtil.parseHead(PaymentOrderExportDTO.class);
            }

            WriteSheet testSheet = EasyExcel.writerSheet("sheet1")
                    .head(head)
                    .build();
            excelWriter.write(result.getData(), testSheet);
            long pageNo = 1;
            long totalSize = 0;
            // startTime <= time < endTime
            if (result.getTotal() > GlobalConstants.BATCH_SIZE && result.getTotal() % GlobalConstants.BATCH_SIZE > 0) {
                totalSize = (result.getTotal() / GlobalConstants.BATCH_SIZE) + 1;
            } else if (result.getTotal() > GlobalConstants.BATCH_SIZE && result.getTotal() % GlobalConstants.BATCH_SIZE <= 0) {
                totalSize = (result.getTotal() / GlobalConstants.BATCH_SIZE);
            }
            for (int i = 0; i < totalSize; i++) {
                pageNo++;
                req.setPageNo(pageNo);
                req.setPageSize(GlobalConstants.BATCH_SIZE);
                RestResult<List<PaymentOrderExportDTO>> resultList = paymentOrderFeignClient.listPageExport(req);
                exportTotalSize = exportTotalSize + resultList.getData().size();
                if(exportTotalSize > GlobalConstants.EXPORT_TOTAL_SIZE){
                    return;
                }
                WriteSheet testSheet1 = EasyExcel.writerSheet("sheet1")
                        .build();
                excelWriter.write(resultList.getData(), testSheet1);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            bos.flush();
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }
    }

    @PostMapping("/manualCallback")
    @ApiOperation(value = "卖出订单手动回调成功")
    @SysLog(title="卖出订单",content = "手动回调成功")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "记录id", required = true, dataType = "Long")
    })
    public RestResult<Boolean> manualCallback(Long id) {
        String opName = UserContext.getCurrentUserName();
        RestResult<Boolean> result = paymentOrderFeignClient.manualCallback(id, opName);
        return result;
    }


    @PostMapping("/listRecordPage")
    @ApiOperation(value = "卖出订单记录")
    public RestResult<List<PaymentOrderListPageDTO>> listRecordPage(@RequestBody(required = false) @ApiParam PaymentOrderListPageReq req) {
        RestResult<List<PaymentOrderListPageDTO>> result = paymentOrderFeignClient.listRecordPage(req);
        return result;
    }


    @PostMapping("/listRecordTotalPage")
    @ApiOperation(value = "卖出订单总计")
    public RestResult<PaymentOrderListPageDTO> listRecordTotalPage(@RequestBody(required = false) @ApiParam PaymentOrderListPageReq req) {
        RestResult<PaymentOrderListPageDTO> result = paymentOrderFeignClient.listRecordTotalPage(req);
        return result;
    }

    @PostMapping("/getInfo")
    @ApiOperation(value = "查看")
    public RestResult<PaymentOrderInfoDTO> getInfo(@RequestBody(required = false) @ApiParam PaymentOrderGetInfoReq req) {
        RestResult<PaymentOrderInfoDTO> result =  paymentOrderFeignClient.getInfo(req);
        return result;
    }


    @PostMapping("/cancel")
    @SysLog(title = "卖出订单控制器",content = "取消订单")
    @ApiOperation(value = "取消订单")
    public RestResult<PaymentOrderListPageDTO> cancel(@RequestBody(required = false) @ApiParam PaymentOrderIdReq req) {
        String opName = UserContext.getCurrentUserName();
        req.setOpName(opName);
        RestResult<PaymentOrderListPageDTO> result = paymentOrderFeignClient.cancel(req);
        return result;
    }



}
