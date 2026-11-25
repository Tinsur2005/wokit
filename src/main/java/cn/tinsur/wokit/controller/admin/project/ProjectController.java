package cn.tinsur.wokit.controller.admin.project;

import cn.tinsur.wokit.pojo.dto.ProjectDTO;
import cn.tinsur.wokit.pojo.vo.ProjectVO;
import cn.tinsur.wokit.service.IProjectService;
import cn.tinsur.wokit.util.AuthUtil;
import cn.tinsur.wokit.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 项目 前端控制器
 * </p>
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/admin/projects")
public class ProjectController {

    @Autowired
    private IProjectService projectService;

    /**
     * 查询项目列表，按创建时间倒序
     * GET /admin/projects?categoryId=1&keyword=Wokit
     */
    @GetMapping
    public Result<List<ProjectVO>> list(@RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) String keyword,
                                        HttpServletRequest request) {
        return Result.ok(projectService.listByUser(AuthUtil.getUserId(request), categoryId, keyword));
    }

    /**
     * 新增项目
     * POST /admin/projects
     */
    @PostMapping
    public Result add(@RequestBody ProjectDTO dto, HttpServletRequest request) {
        return projectService.add(dto, AuthUtil.getUserId(request));
    }

    /**
     * 修改项目
     * PUT /admin/projects/1
     */
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody ProjectDTO dto, HttpServletRequest request) {
        return projectService.update(id, dto, AuthUtil.getUserId(request));
    }

    /**
     * 删除项目（真实删除，并解除业务记录绑定）
     * DELETE /admin/projects/1
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        return projectService.delete(id, AuthUtil.getUserId(request));
    }
}
