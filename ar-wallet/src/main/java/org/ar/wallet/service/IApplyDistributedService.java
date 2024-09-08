package org.ar.wallet.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.ApplyDistributedDTO;
import org.ar.common.pay.req.ApplyDistributedListPageReq;
import org.ar.wallet.entity.ApplyDistributed;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.wallet.req.ApplyDistributedReq;


/**
* @author 
*/
    public interface IApplyDistributedService extends IService<ApplyDistributed> {

       PageReturn<ApplyDistributedDTO> listPage(ApplyDistributedListPageReq req);

       PageReturn<ApplyDistributedDTO> listRecordPage(ApplyDistributedListPageReq req);


       ApplyDistributedDTO listRecordTotal(ApplyDistributedListPageReq req);



       ApplyDistributedDTO  distributed(ApplyDistributed applyDistributed);



}
