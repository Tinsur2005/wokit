package cn.tinsur.wokit.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 工时统计卡片数据
 *
 * @author Tinsur
 */
@Data
public class WorkLogStatVO {

    /**
     * 本月累计工时
     */
    private BigDecimal monthHours;

    /**
     * 本月登记天数
     */
    private Integer days;

    /**
     * 日均工时
     */
    private BigDecimal avgHours;

    /**
     * 本月登记条数
     */
    private Integer records;
}
