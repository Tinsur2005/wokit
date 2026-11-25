package cn.tinsur.wokit.pojo.dto;

import lombok.Data;

/**
 * 项目分类入参
 *
 * @author Tinsur
 */
@Data
public class ProjectCategoryDTO {

    /**
     * 分类名称
     */
    private String name;

    /**
     * 父分类ID，空表示一级分类
     */
    private Long parentId;
}
