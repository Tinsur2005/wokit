package cn.tinsur.wokit.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 项目工时
 *
 * @author Tinsur
 */
@Data
public class ProjectHourVO {

    /**
     * 项目名称
     */
    private String project;

    /**
     * 本月累计工时
     */
    private BigDecimal hours;
}
