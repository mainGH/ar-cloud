package org.ar.wallet.service;

import org.ar.wallet.vo.TestImageRecognitionVo;

public interface ImageRecognitionService {

    /**
     * 识别是否是 支付凭证截图
     *
     * @param imagePath
     * @return {@link TestImageRecognitionVo}
     */
    TestImageRecognitionVo isPaymentVoucher(String imagePath);
}
