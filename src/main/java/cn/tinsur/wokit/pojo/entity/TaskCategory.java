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
 * 项目任务分类。分类由管理员在系统设置中统一维护，所有用户共用。
 *
 * @author Tinsur
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("work_task_category")
public class TaskCategory implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分类名称，例如新需求、BUG 修复
     */
    private String name;

    /**
     * 标签颜色，十六进制颜色值
     */
    private String color;

    /**
     * 排序值，越小越靠前
     */
    private Integer sort;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
