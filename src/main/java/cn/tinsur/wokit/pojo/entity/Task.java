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
import java.util.Date;

/**
 * <p>
 * 每日任务表
 * </p>
 *
 * @author Tinsur
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("work_task")
public class Task implements Serializable {

    /**
     * 任务ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 开始时间
     */
    @TableField("start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    /**
     * 截止时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date deadline;

    /**
     * 优先级（0：紧急，1：重要，2：普通，3：低优先级）
     */
    private Integer priority;

    /**
     * 任务状态（0：待处理，1：进行中，2：阻塞中，3：已完成）
     */
    private Integer status;

    /**
     * 任务类型（0：普通任务，1：项目任务）
     */
    @TableField("task_kind")
    private Integer taskKind;

    /**
     * 绑定项目ID，仅项目任务使用
     */
    @TableField("project_id")
    private Long projectId;

    /**
     * 项目任务分类ID，创建后不可修改
     */
    @TableField("task_category_id")
    private Long taskCategoryId;

    /**
     * 实际完成时间，用于周报和统计
     */
    @TableField("completed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date completedAt;

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
