package org.ar.pay.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


@Data
public class SysRoleSelectVO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
    * id
    */
    private Long id;

    /**
    * 角色名称
    */
    private String name;
}