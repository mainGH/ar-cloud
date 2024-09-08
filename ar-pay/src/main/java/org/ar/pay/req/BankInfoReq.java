package org.ar.pay.req;

import org.ar.common.core.page.PageRequest;

import java.io.Serializable;

public class BankInfoReq  extends PageRequest {



        /**
         * 三方编码
         */
        private String thirdCode;

        /**
         * guo'j
         */
        private Integer county;

        /**
         * 银行编码
         */
        private String bankCode;

        /**
         * 银行名称
         */
        private String bankName;

    }
