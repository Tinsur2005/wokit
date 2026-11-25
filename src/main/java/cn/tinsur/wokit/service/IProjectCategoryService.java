package cn.tinsur.wokit.service;

import cn.tinsur.wokit.pojo.dto.ProjectCategoryDTO;
import cn.tinsur.wokit.pojo.entity.ProjectCategory;
import cn.tinsur.wokit.pojo.vo.ProjectCategoryVO;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IProjectCategoryService extends IService<ProjectCategory> {

    /**
     * 查询当前用户的全部项目分类
     */
    List<ProjectCategoryVO> listByUser(Long userId);

    /**
     * 新增分类
     */
    Result add(ProjectCategoryDTO dto, Long userId);

    /**
     * 修改分类
     */
    Result update(Long id, ProjectCategoryDTO dto, Long userId);

    /**
     * 删除分类
     */
    Result delete(Long id, Long userId);
}
