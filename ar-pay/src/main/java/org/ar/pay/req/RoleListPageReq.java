package org.ar.pay.req;


import lombok.Data;
import org.ar.common.core.page.PageRequest;

@Data
public class RoleListPageReq extends PageRequest {
    private String keyword;
}
