package org.ar.pay.entity;

import lombok.Data;

import java.io.Serializable;


@Data
public class MerchantRole implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
    * 用户id
    */
    private Long merchantId;

    /**
    * 角色id
    */
    private Long roleId;

    public MerchantRole() {}

    public MerchantRole(Long merchantId, Long roleId) {
        this.merchantId = merchantId;
        this.roleId = roleId;
    }
}