package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.service.IService;

import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.TradeConfigDTO;
import org.ar.common.pay.dto.TradeConfigVoiceEnableDTO;
import org.ar.common.pay.dto.TradeManualConfigDTO;
import org.ar.common.pay.dto.TradeWarningConfigDTO;
import org.ar.common.pay.req.*;
import org.ar.wallet.entity.TradeConfig;


/**
 * @author
 */
public interface ITradeConfigService extends IService<TradeConfig> {

    PageReturn<TradeConfigDTO> listPage(TradeConfigListPageReq req);

    TradeConfigVoiceEnableDTO updateVoiceEnable(TradeConfigVoiceEnableReq req);

    TradeWarningConfigDTO updateWarningConfig(TradeConfigWarningConfigUpdateReq req);

    TradeWarningConfigDTO warningConfigDetail(TradeConfigIdReq req);

    TradeManualConfigDTO manualReview();

}
