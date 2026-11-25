package cn.tinsur.wokit.service;

import cn.tinsur.wokit.pojo.dto.ProjectDTO;
import cn.tinsur.wokit.pojo.entity.Project;
import cn.tinsur.wokit.pojo.vo.ProjectVO;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IProjectService extends IService<Project> {

    /**
     * 查询当前用户的项目，按创建时间倒序
     */
    List<ProjectVO> listByUser(Long userId, Long categoryId, String keyword);

    /**
     * 新增项目
     */
    Result add(ProjectDTO dto, Long userId);

    /**
     * 修改项目
     */
    Result update(Long id, ProjectDTO dto, Long userId);

    /**
     * 删除项目并解除业务记录上的绑定
     */
    Result delete(Long id, Long userId);
}
