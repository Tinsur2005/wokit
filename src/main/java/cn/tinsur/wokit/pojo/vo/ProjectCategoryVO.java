package cn.tinsur.wokit.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 项目分类出参
 *
 * @author Tinsur
 */
@Data
public class ProjectCategoryVO {

    private Long id;

    private String name;

    private Long parentId;

    /**
     * 从一级分类到当前分类的完整路径
     */
    private String path;

    private Integer projectCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
