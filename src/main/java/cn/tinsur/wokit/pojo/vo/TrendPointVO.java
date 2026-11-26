package cn.tinsur.wokit.pojo.vo;

import lombok.Data;

/**
 * 任务趋势点
 *
 * @author Tinsur
 */
@Data
public class TrendPointVO {

    /**
     * 日期（yyyy-MM-dd）
     */
    private String date;

    /**
     * 坐标轴短标签（MM-dd）
     */
    private String label;

    /**
     * 当天新增任务数
     */
    private Integer created;

    /**
     * 当天任务总数（截止日覆盖当天且当天已开始）
     */
    private Integer total;

    /**
     * 当天完成任务数
     */
    private Integer completed;
}
