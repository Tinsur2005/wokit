package cn.tinsur.wokit.pojo.query;

import lombok.Data;

@Data
public class WorkLogQuery {

    /**
     * 月份（yyyy-MM）
     */
    private String month;

    /**
     * 项目或工作内容关键字
     */
    private String keyword;

    private Integer page;

    private Integer limit;
}
