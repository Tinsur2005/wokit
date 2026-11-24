package cn.tinsur.wokit.pojo.vo;

import lombok.Data;

/**
 * 周报趋势点
 *
 * @author Tinsur
 */
@Data
public class WeeklyTrendVO {

    /**
     * 周起始日（yyyy-MM-dd）
     */
    private String weekStart;

    /**
     * 坐标轴标签
     */
    private String label;

    /**
     * 当周周报记录数
     */
    private Integer count;
}
