package org.ar.pay.vo;


import lombok.Data;

import java.util.List;


@Data
public class SysUserVO {

    private Long id;

    private String username;

    private String nickname;

    private String mobile;

    private Integer gender;

    private String avatar;

    private String email;

    private Integer status;

    private List<Long> menuIds;
    private List<Long> roleIds;
    private List<String> permissions;

}
