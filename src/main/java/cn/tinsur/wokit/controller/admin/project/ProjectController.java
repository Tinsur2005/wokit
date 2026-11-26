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
     * 查询项目列表，按创建时间倒序。
     * GET /admin/projects?categoryId=1&keyword=Wokit&status=1&archived=false
     */
    @GetMapping
    public Result<List<ProjectVO>> list(@RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Integer status,
                                        @RequestParam(required = false) Boolean archived,
                                        HttpServletRequest request) {
        return Result.ok(projectService.listByUser(
                AuthUtil.getUserId(request), categoryId, keyword, status, archived));
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
     * 修改项目状态
     * PUT /admin/projects/status/1?status=2
     */
    @PutMapping("/status/{id}")
    public Result changeStatus(@PathVariable Long id,
                               @RequestParam Integer status,
                               HttpServletRequest request) {
        return projectService.changeStatus(id, status, AuthUtil.getUserId(request));
    }

    /**
     * 归档项目
     * PUT /admin/projects/archive/1?reason=已交付，进入长期维护
     */
    @PutMapping("/archive/{id}")
    public Result archive(@PathVariable Long id,
                          @RequestParam(required = false) String reason,
                          HttpServletRequest request) {
        return projectService.archive(id, reason, AuthUtil.getUserId(request));
    }

    /**
     * 恢复已归档项目
     * PUT /admin/projects/restore/1
     */
    @PutMapping("/restore/{id}")
    public Result restore(@PathVariable Long id, HttpServletRequest request) {
        return projectService.restore(id, AuthUtil.getUserId(request));
    }

    /**
     * 删除项目。存在关联任务、周报或工时时只能归档。
     * DELETE /admin/projects/1
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        return projectService.delete(id, AuthUtil.getUserId(request));
    }
}
