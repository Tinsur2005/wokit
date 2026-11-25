package cn.tinsur.wokit.pojo.vo;

import lombok.Data;

/**
 * 任务统计卡片数据
 *
 * @author Tinsur
 */
@Data
public class TaskStatVO {

    /**
     * 今日未完成数量
     */
    private Integer todayPending;

    /**
     * 今日截止任务总数
     */
    private Integer todayTotal;

    /**
     * 本周已完成数量
     */
    private Integer weekCompleted;

    /**
     * 本月任务总数
     */
    private Integer monthTotal;

    /**
     * 本月已完成数量
     */
    private Integer monthCompleted;
}
