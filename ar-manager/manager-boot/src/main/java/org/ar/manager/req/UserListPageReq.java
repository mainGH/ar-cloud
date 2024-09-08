package org.ar.manager.req;


import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.ar.common.core.page.PageRequest;


@Data
@ApiModel("用户查询参数")
public class UserListPageReq extends PageRequest {
    /**
     * 关键字
     */
    private String keyword;
    /**
     * 用户名
     */
    private String username;
    /**
     * 角色
     */
    private String role;

}
