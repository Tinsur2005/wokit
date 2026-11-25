package cn.tinsur.wokit.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 工时登记表
 * </p>
 *
 * @author Tinsur
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("work_log")
public class WorkLog implements Serializable {

    /**
     * 工时记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 工作日期
     */
    @TableField("work_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date workDate;

    /**
     * 事项名称
     */
    private String project;

    /**
     * 绑定项目ID
     */
    @TableField("project_id")
    private Long projectId;

    /**
     * 工作内容
     */
    private String content;

    /**
     * 工时（小时）
     */
    private BigDecimal hours;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
