package cn.tinsur.wokit.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统权限表：路由目录、菜单、按钮统一挂在一棵树上
 *
 * @author Tinsur
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_permission")
public class Permission implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 父权限ID，0 表示根节点
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 类型（0：路由/目录，1：菜单，2：按钮）
     */
    private Integer type;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 按钮权限标识
     */
    @TableField("permission_value")
    private String permissionValue;

    /**
     * 菜单图标名称
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态（0：停用，1：启用）
     */
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
