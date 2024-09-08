package org.ar.pay.vo;


import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;


@Data
@Accessors(chain = true)
public class SysPermissionVO implements Serializable {

    private Long id;

    private String name;

    private Long menuId;

    private String urlPerm;

    private String btnSign;

}
