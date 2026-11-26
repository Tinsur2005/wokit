package cn.tinsur.wokit.service;

import cn.tinsur.wokit.pojo.dto.ProjectDTO;
import cn.tinsur.wokit.pojo.entity.Project;
import cn.tinsur.wokit.pojo.vo.ProjectVO;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IProjectService extends IService<Project> {

    /**
     * 查询当前用户的项目，按创建时间倒序。
     *
     * @param archived true 查已归档，false 查未归档
     */
    List<ProjectVO> listByUser(Long userId, Long categoryId, String keyword,
                               Integer status, Boolean archived);

    /**
     * 新增项目
     */
    Result add(ProjectDTO dto, Long userId);

    /**
     * 修改项目。已归档项目不可修改。
     */
    Result update(Long id, ProjectDTO dto, Long userId);

    /**
     * 删除项目。存在任务、周报或工时时禁止删除，只能归档。
     */
    Result delete(Long id, Long userId);

    /**
     * 修改项目业务状态。已归档项目不可修改。
     */
    Result changeStatus(Long id, Integer status, Long userId);

    /**
     * 归档项目。归档后项目只读，可恢复。
     */
    Result archive(Long id, String reason, Long userId);

    /**
     * 恢复已归档项目，回到归档前的业务状态。
     */
    Result restore(Long id, Long userId);
}
