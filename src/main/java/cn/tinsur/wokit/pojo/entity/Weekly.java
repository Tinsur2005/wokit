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
 * 周报记录表
 * </p>
 *
 * @author Tinsur
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("work_weekly")
public class Weekly implements Serializable {

    /**
     * 周报记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 周起始日期（周一）
     */
    @TableField("week_start")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date weekStart;

    /**
     * 周结束日期（周日）
     */
    @TableField("week_end")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date weekEnd;

    /**
     * 任务内容
     */
    @TableField("task_content")
    private String taskContent;

    /**
     * 任务结果
     */
    @TableField("task_result")
    private String taskResult;

    /**
     * 存在的问题
     */
    private String problem;

    /**
     * 产出
     */
    private String output;

    /**
     * 其他补充
     */
    private String extra;

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
