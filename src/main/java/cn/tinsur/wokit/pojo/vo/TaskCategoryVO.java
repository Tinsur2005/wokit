package cn.tinsur.wokit.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 任务分类出参，附带已被多少条任务使用
 *
 * @author Tinsur
 */
@Data
public class TaskCategoryVO {

    private Long id;

    private String name;

    private String color;

    private Integer sort;

    /**
     * 已使用该分类的任务数，用于判断是否允许删除
     */
    private Long usageCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
