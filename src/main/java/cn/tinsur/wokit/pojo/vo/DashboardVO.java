package cn.tinsur.wokit.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 首页数据看板
 *
 * @author Tinsur
 */
@Data
public class DashboardVO {

    /**
     * 今日待办数量
     */
    private Integer todayPending;

    /**
     * 今日截止任务总数
     */
    private Integer todayTotal;

    /**
     * 本周已完成任务数
     */
    private Integer weekCompleted;

    /**
     * 本月累计工时
     */
    private BigDecimal monthHours;

    /**
     * 进行中的计时数量
     */
    private Integer runningTimers;

    /**
     * 近 7 天任务趋势
     */
    private List<TrendPointVO> taskTrend;

    /**
     * 本月工时项目分布
     */
    private List<ProjectHourVO> projectHours;

    /**
     * 近 4 周周报数量
     */
    private List<WeeklyTrendVO> weeklyTrend;
}
