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
 * <p>
 * 项目表
 * </p>
 *
 * @author Tinsur
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("work_project")
public class Project implements Serializable {

    /**
     * 项目ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 分类ID
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 项目名称
     */
    private String name;

    /**
     * 项目状态（0：规划中，1：进行中，2：维护中，3：暂停，4：已完成，5：已取消）
     */
    private Integer status;

    /**
     * 状态最近变更时间
     */
    @TableField("status_changed_at")
    private Date statusChangedAt;

    /**
     * 进入维护中状态的时间，即项目已发布、仍在持续维护
     */
    @TableField("released_at")
    private Date releasedAt;

    /**
     * 完成时间
     */
    @TableField("completed_at")
    private Date completedAt;

    /**
     * 归档时间；非空即为已归档，归档是独立字段，不是业务状态
     */
    @TableField("archived_at")
    private Date archivedAt;

    /**
     * 归档原因
     */
    @TableField("archive_reason")
    private String archiveReason;

    /**
     * 归档前的业务状态，恢复时使用
     */
    @TableField("status_before_archive")
    private Integer statusBeforeArchive;

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
