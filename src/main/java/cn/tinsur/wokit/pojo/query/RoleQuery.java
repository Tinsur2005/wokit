package cn.tinsur.wokit.pojo.query;

import lombok.Data;

/**
 * 角色分页查询参数
 *
 * @author Tinsur
 */
@Data
public class RoleQuery {

    private String name;

    private String code;

    private Integer page;

    private Integer limit;
}
