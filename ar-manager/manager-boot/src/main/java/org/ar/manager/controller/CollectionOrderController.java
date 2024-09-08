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
import org.ar.common.pay.req.CollectionOrderGetInfoReq;
import org.ar.common.pay.req.CollectionOrderIdReq;
import org.ar.common.pay.req.CollectionOrderListPageReq;
import org.ar.common.pay.req.MatchingOrderReq;
import org.ar.common.web.utils.UserContext;
import org.ar.manager.annotation.SysLog;
import org.ar.manager.api.CollectionOrderFeignClient;

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
@RequestMapping(value = {"/api/v1/collectionOrderAdmin", "/collectionOrderAdmin"})
@Api(description = "买入订单控制器")
public class CollectionOrderController {

    private final CollectionOrderFeignClient collectionOrderFeignClient;



    @PostMapping("/listRecordPage")
    @ApiOperation(value = "买入订单记录")
    public RestResult<List<CollectionOrderDTO>> listRecordPage(@RequestBody(required = false) @ApiParam CollectionOrderListPageReq collectionOrderReq) {
        RestResult<List<CollectionOrderDTO>> result = collectionOrderFeignClient.listRecordPage(collectionOrderReq);
        return result;
    }


    @PostMapping("/pay")
    @SysLog(title = "代收订单控制器",content = "支付接口")
    @ApiOperation(value = "已支付")
    public RestResult<CollectionOrderDTO> pay(@RequestBody(required = false) @ApiParam CollectionOrderIdReq req) {
        String opName = UserContext.getCurrentUserName();
        req.setCompletedBy(opName);
        RestResult<CollectionOrderDTO> result = collectionOrderFeignClient.pay(req);
        return result;
    }

    @PostMapping("/manualCallback")
    @ApiOperation(value = "买入订单手动回调成功")
    @SysLog(title="买入订单",content = "手动回调成功")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "记录id", required = true, dataType = "Long")
    })
    public RestResult<Boolean> manualCallback(Long id) {
        String opName = UserContext.getCurrentUserName();
        RestResult<Boolean> result = collectionOrderFeignClient.manualCallback(id, opName);
        return result;
    }


    @PostMapping("/listPage")
    @ApiOperation(value = "买入订单")
    public RestResult<List<CollectionOrderDTO>> listPage(@RequestBody(required = false) @ApiParam CollectionOrderListPageReq collectionOrderReq) {
        RestResult<List<CollectionOrderDTO>> result = collectionOrderFeignClient.listPage(collectionOrderReq);
        return result;
    }

    @PostMapping("/export")
    @ApiOperation(value = "买入订单列表导出")
    public void export(HttpServletResponse response, @RequestBody @ApiParam CollectionOrderListPageReq matchingOrderReq) throws IOException {
        matchingOrderReq.setPageSize(GlobalConstants.BATCH_SIZE);
        RestResult<List<CollectionOrderExportDTO>> result = collectionOrderFeignClient.listPageExport(matchingOrderReq);
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
            if (matchingOrderReq.getLang().equals("zh")) {
                ExcelUtil.setResponseHeader(response, "BuyOrderRecords");
                excelWriter = EasyExcel.write(bos, CollectionOrderExportDTO.class).build();
                head = ExcelUtil.parseHead(CollectionOrderExportDTO.class);
            } else {
                ExcelUtil.setResponseHeader(response, "BuyOrderRecords");
                excelWriter = EasyExcel.write(bos, CollectionOrderExportDTO.class).build();
                head = ExcelUtil.parseHead(CollectionOrderExportDTO.class);
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
                matchingOrderReq.setPageNo(pageNo);
                matchingOrderReq.setPageSize(GlobalConstants.BATCH_SIZE);
                RestResult<List<CollectionOrderExportDTO>> resultList = collectionOrderFeignClient.listPageExport(matchingOrderReq);
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


    @PostMapping("/listPageRecordTotal")
    @ApiOperation(value = "买入订单记录汇总")
    public RestResult<CollectionOrderDTO> listPageRecordTotal(@RequestBody(required = false) @ApiParam CollectionOrderListPageReq collectionOrderReq) {
        RestResult<CollectionOrderDTO> result = collectionOrderFeignClient.listPageRecordTotal(collectionOrderReq);
        return result;
    }

    @PostMapping("/getInfo")
    @ApiOperation(value = "查看")
    public RestResult<CollectionOrderInfoDTO> getInfo(@RequestBody(required = false) @ApiParam CollectionOrderGetInfoReq req) {
        RestResult<CollectionOrderInfoDTO> result = collectionOrderFeignClient.getInfo(req);
        return result;
    }




}
