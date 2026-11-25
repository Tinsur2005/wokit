package cn.tinsur.wokit.pojo.dto;

import lombok.Data;

/**
 * 项目入参
 *
 * @author Tinsur
 */
@Data
public class ProjectDTO {

    /**
     * 项目名称
     */
    private String name;

    /**
     * 分类ID
     */
    private Long categoryId;
}
