package org.ar.manager.api;

import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.TaskCollectionRecordDTO;
import org.ar.common.pay.dto.TaskManagerDTO;
import org.ar.common.pay.req.TaskCollectionRecordReq;
import org.ar.common.pay.req.TaskManagerIdReq;
import org.ar.common.pay.req.TaskManagerListReq;
import org.ar.common.pay.req.TaskManagerReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author admin
 * @date 2024/3/19 9:52
 */
@FeignClient(value = "ar-wallet", contextId = "task-collection-record")
public interface TaskCollectionRecordClient {

    @PostMapping("/api/v1/taskCollectionRecord/listPage")
    PageReturn<TaskCollectionRecordDTO> listPage(@RequestBody TaskCollectionRecordReq req);
    @PostMapping("/api/v1/taskCollectionRecord/getStatisticsData")
    TaskCollectionRecordDTO getStatisticsData();
}
