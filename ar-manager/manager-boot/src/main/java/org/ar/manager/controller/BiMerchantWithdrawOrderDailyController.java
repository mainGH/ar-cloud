package org.ar.manager.controller;


import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.utils.ExcelUtil;
import org.ar.common.pay.dto.BiMerchantWithdrawOrderDailyDTO;
import org.ar.common.pay.dto.BiMerchantWithdrawOrderDailyEnDTO;
import org.ar.common.pay.dto.BiWithdrawEnOrderDailyDTO;
import org.ar.common.pay.dto.BiWithdrawOrderDailyExportDTO;
import org.ar.manager.entity.BiMerchantPayOrderDaily;
import org.ar.manager.entity.BiMerchantWithdrawOrderDaily;
import org.ar.manager.req.MerchantMonthReportReq;
import org.ar.manager.req.WithdrawDailyOrderReportReq;
import org.ar.manager.service.IBiMerchantPayOrderDailyService;
import org.ar.manager.service.IBiMerchantWithdrawOrderDailyService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * @author
 */
@Slf4j
@RestController
@Api(description = "商户代付日报表控制器")
@RequestMapping(value = {"/api/v1/biMerchantWithdrawOrderDaily", "/biMerchantWithdrawOrderDaily"})
public class BiMerchantWithdrawOrderDailyController {

    @Resource
    IBiMerchantWithdrawOrderDailyService iBiMerchantMonthService;

    @PostMapping("/query")
    @ApiOperation(value = "查询商户代付报日表记录")
    public RestResult<List<BiMerchantWithdrawOrderDaily>> listPage(@Validated @RequestBody MerchantMonthReportReq req) {

        PageReturn<BiMerchantWithdrawOrderDaily> result = iBiMerchantMonthService.listPage(req);
        return RestResult.page(result);
    }

    @PostMapping("/export")
    @ApiOperation(value = "商户代付报日表导出")
    public void export(HttpServletResponse response, @RequestBody @ApiParam MerchantMonthReportReq req) throws IOException {
        req.setPageSize(GlobalConstants.BATCH_SIZE);
        PageReturn<BiMerchantWithdrawOrderDailyDTO> result = iBiMerchantMonthService.listPageForExport(req);
        OutputStream outputStream;
        BufferedOutputStream bos = null;
        ExcelWriter excelWriter = null;
        int exportTotalSize;
        String fileName = "BiMerchantWithdrawOrderDaily";
        String sheetName = "sheet1";
        try{
            outputStream = response.getOutputStream();
            exportTotalSize = result.getList().size();
            // 写入head
            List<List<String>> head;
            bos = new BufferedOutputStream(outputStream);
            Class<?> clazz = BiMerchantWithdrawOrderDailyDTO.class;
            ExcelUtil.setResponseHeader(response, fileName);
            if (!"zh".equals(req.getLang())) {
                clazz = BiMerchantWithdrawOrderDailyEnDTO.class;
            }
            excelWriter = EasyExcel.write(bos, clazz).build();
            head = ExcelUtil.parseHead(clazz);
            WriteSheet testSheet = EasyExcel.writerSheet(sheetName)
                    .head(head)
                    .build();
            excelWriter.write(result.getList(), testSheet);
            // 写入数据
            long pageNo = 1;
            long totalSize = ExcelUtil.getTotalSize(result.getTotal());
            for (int i = 0; i < totalSize; i++) {
                pageNo++;
                req.setPageNo(pageNo);
                req.setPageSize(GlobalConstants.BATCH_SIZE);
                PageReturn<BiMerchantWithdrawOrderDailyDTO> resultList = iBiMerchantMonthService.listPageForExport(req);
                exportTotalSize = exportTotalSize + resultList.getList().size();
                if(exportTotalSize > GlobalConstants.EXPORT_TOTAL_SIZE){
                    return;
                }
                WriteSheet testSheet1 = EasyExcel.writerSheet(sheetName)
                        .build();
                excelWriter.write(resultList.getList(), testSheet1);
            }
        }catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            assert bos != null;
            bos.flush();
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }
    }
}
