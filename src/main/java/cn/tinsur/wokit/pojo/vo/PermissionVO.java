package cn.tinsur.wokit.pojo.vo;

import cn.tinsur.wokit.pojo.entity.Permission;
import lombok.Data;

import java.util.List;

/**
 * 权限树节点
 *
 * @author Tinsur
 */
@Data
public class PermissionVO extends Permission {

    private List<PermissionVO> children;
}
