package org.ar.manager.controller;


import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.constant.RedisConstants;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.utils.CommonUtils;
import org.ar.common.core.utils.ExcelUtil;
import org.ar.common.pay.dto.BiMerchantDailyDTO;
import org.ar.common.pay.dto.BiMerchantDailyEnDTO;
import org.ar.common.redis.util.RedisUtils;
import org.ar.manager.entity.BiMerchantMonth;
import org.ar.manager.req.MerchantDailyReportReq;
import org.ar.manager.req.MerchantMonthReportReq;
import org.ar.manager.service.IBiMerchantMonthService;
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
@RestController
@Slf4j
@Api(description = "商户月报表控制器")
@RequestMapping(value = {"/api/v1/biMerchantMonth", "/biMerchantMonth"})
public class BiMerchantMonthController {

    @Resource
    IBiMerchantMonthService iBiMerchantMonthService;

    @PostMapping("/query")
    @ApiOperation(value = "查询商户月报表记录")
    public RestResult<List<BiMerchantMonth>> listPage(@Validated @RequestBody MerchantMonthReportReq req) {

        PageReturn<BiMerchantMonth> result = iBiMerchantMonthService.listPage(req);
        return RestResult.page(result);
    }

    @PostMapping("/export")
    @ApiOperation(value = "查询商户月报表记录")
    public void export(HttpServletResponse response, @RequestBody @ApiParam MerchantMonthReportReq req) throws IOException {
        req.setPageSize(GlobalConstants.BATCH_SIZE);
        PageReturn<BiMerchantDailyDTO> result = iBiMerchantMonthService.listPageForExport(req);
        OutputStream outputStream;
        BufferedOutputStream bos = null;
        ExcelWriter excelWriter = null;
        int exportTotalSize;
        String fileName = "BiMerchantRecords";
        String sheetName = "sheet1";
        try{
            outputStream = response.getOutputStream();
            exportTotalSize = result.getList().size();
            // 写入head
            List<List<String>> head;
            bos = new BufferedOutputStream(outputStream);
            Class<?> clazz = BiMerchantDailyDTO.class;
            ExcelUtil.setResponseHeader(response, fileName);
            if (!"zh".equals(req.getLang())) {
                clazz = BiMerchantDailyEnDTO.class;
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
                PageReturn<BiMerchantDailyDTO> resultList = iBiMerchantMonthService.listPageForExport(req);
                exportTotalSize = exportTotalSize +resultList.getList().size();
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
