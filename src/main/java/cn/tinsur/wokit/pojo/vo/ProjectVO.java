package cn.tinsur.wokit.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 项目出参
 *
 * @author Tinsur
 */
@Data
public class ProjectVO {

    private Long id;

    private String name;

    private Long categoryId;

    private String categoryName;

    /**
     * 业务状态：0 规划中、1 进行中、2 维护中、3 暂停、4 已完成、5 已取消
     */
    private Integer status;

    private String statusName;

    /**
     * 是否已归档。归档后项目只读，可恢复。
     */
    private Boolean archived;

    private String archiveReason;

    /**
     * 项目任务总数（task_kind = 1）
     */
    private Long taskTotal;

    /**
     * 未完成项目任务数（status != 3）
     */
    private Long taskOpen;

    /**
     * 已完成项目任务数
     */
    private Long taskDone;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date statusChangedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date releasedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date completedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date archivedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
