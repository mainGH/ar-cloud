package org.ar.pay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;


@Data
public class SysMenu extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    /**
    * id
    */
    private Long id;

    /**
    * 菜单名称
    */
    private String name;

    /**
    * 父菜单id
    */
    private Long parentId;

    /**
    * 路由路径
    */
    private String path;

    /**
    * 组件路径
    */
    private String component;

    /**
    * 菜单图标
    */
    private String icon;

    /**
    * 排序
    */
    private Integer sort;

    /**
    * 状态：0-禁用 1-开启
    */
    private int visible;

    /**
    * 跳转路径
    */
    private String redirect;


    public SysMenu() {}
}