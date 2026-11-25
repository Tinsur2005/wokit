package cn.tinsur.wokit.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 项目出参
 *
 * @author Tinsur
 */
@Data
public class ProjectVO {

    private Long id;

    private String name;

    private Long categoryId;

    private String categoryName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
