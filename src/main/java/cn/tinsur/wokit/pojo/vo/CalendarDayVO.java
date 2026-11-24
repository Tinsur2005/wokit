package cn.tinsur.wokit.pojo.vo;

import lombok.Data;

/**
 * 日历某一天的任务标记
 *
 * @author Tinsur
 */
@Data
public class CalendarDayVO {

    /**
     * 日期（yyyy-MM-dd）
     */
    private String date;

    /**
     * 当日任务总数
     */
    private Integer total;

    /**
     * 当日未完成任务数
     */
    private Integer pending;
}
