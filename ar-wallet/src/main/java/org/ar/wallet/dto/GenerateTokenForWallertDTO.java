package org.ar.wallet.dto;


import lombok.Data;
import org.ar.common.core.result.ResultCode;

import java.io.Serializable;

@Data
public class GenerateTokenForWallertDTO implements Serializable {

    /**
     * requestIp
     */
    private String requestIp;

    /**
     * memberId
     */
    private String memberId;

    /**
     * mobileNumber
     */
    private String mobileNumber;

    /**
     * mobileNumber
     */
    private String memberAccount;

    /**
     * resultCode
     */
    private ResultCode resultCode;
}
