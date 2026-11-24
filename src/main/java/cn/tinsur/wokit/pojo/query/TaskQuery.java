package cn.tinsur.wokit.pojo.query;

import lombok.Data;

@Data
public class TaskQuery {

    /**
     * 标题或描述关键字
     */
    private String keyword;

    /**
     * 任务状态（0：待完成，1：已完成）
     */
    private Integer status;

    /**
     * 截止日期（yyyy-MM-dd），按当日截止任务查询
     */
    private String date;

    /**
     * 月份（yyyy-MM），用于日历标记
     */
    private String month;

    private Integer page;

    private Integer limit;
}
