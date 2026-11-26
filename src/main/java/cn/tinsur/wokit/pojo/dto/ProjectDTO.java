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

    /**
     * 编辑项目时可同时修改业务状态（0-5）
     */
    private Integer status;
}
