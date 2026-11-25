package cn.tinsur.wokit.controller.admin.project;

import cn.tinsur.wokit.pojo.dto.ProjectCategoryDTO;
import cn.tinsur.wokit.pojo.vo.ProjectCategoryVO;
import cn.tinsur.wokit.service.IProjectCategoryService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 项目分类 前端控制器
 * </p>
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/admin/project-categories")
public class ProjectCategoryController {

    @Autowired
    private IProjectCategoryService projectCategoryService;

    /**
     * 查询项目分类列表
     * GET /admin/project-categories
     */
    @GetMapping
    public Result<List<ProjectCategoryVO>> list(HttpServletRequest request) {
        return Result.ok(projectCategoryService.listByUser(AuthUtil.getUserId(request)));
    }

    /**
     * 新增项目分类
     * POST /admin/project-categories
     */
    @PostMapping
    public Result add(@RequestBody ProjectCategoryDTO dto, HttpServletRequest request) {
        return projectCategoryService.add(dto, AuthUtil.getUserId(request));
    }

    /**
     * 修改项目分类
     * PUT /admin/project-categories/1
     */
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody ProjectCategoryDTO dto, HttpServletRequest request) {
        return projectCategoryService.update(id, dto, AuthUtil.getUserId(request));
    }

    /**
     * 删除项目分类
     * DELETE /admin/project-categories/1
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        return projectCategoryService.delete(id, AuthUtil.getUserId(request));
    }
}
