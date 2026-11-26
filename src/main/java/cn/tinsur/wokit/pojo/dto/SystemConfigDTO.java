package cn.tinsur.wokit.pojo.dto;

import lombok.Data;

/**
 * 系统信息保存参数
 *
 * @author Tinsur
 */
@Data
public class SystemConfigDTO {

    /**
     * 系统名称
     */
    private String systemName;

    /**
     * 系统副标题
     */
    private String systemSubName;

    /**
     * 浏览器标题后缀
     */
    private String browserTitle;

    /**
     * 登录页表单说明
     */
    private String loginHint;

    /**
     * 新建任务的默认开始时间，只保存时分，例如 09:00
     */
    private String taskDefaultStartTime;

    /**
     * 新建任务的默认截止时间，只保存时分，例如 18:30
     */
    private String taskDefaultEndTime;
}
