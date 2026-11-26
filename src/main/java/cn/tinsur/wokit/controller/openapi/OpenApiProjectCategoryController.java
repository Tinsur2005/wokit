package cn.tinsur.wokit.controller.openapi;

import cn.tinsur.wokit.pojo.dto.ProjectCategoryDTO;
import cn.tinsur.wokit.pojo.vo.ProjectCategoryVO;
import cn.tinsur.wokit.security.RequirePermission;
import cn.tinsur.wokit.service.IProjectCategoryService;
import cn.tinsur.wokit.util.ApiAuthUtil;
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
 * 开放 API：项目分类
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/api/v1/project-categories")
public class OpenApiProjectCategoryController {

    @Autowired
    private IProjectCategoryService projectCategoryService;

    @GetMapping
    public Result<List<ProjectCategoryVO>> list(HttpServletRequest request) {
        return Result.ok(projectCategoryService.listByUser(ApiAuthUtil.getUserId(request)));
    }

    @PostMapping
    @RequirePermission("project:category:add")
    public Result add(@RequestBody ProjectCategoryDTO dto, HttpServletRequest request) {
        return projectCategoryService.add(dto, ApiAuthUtil.getUserId(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("project:category:edit")
    public Result update(@PathVariable Long id, @RequestBody ProjectCategoryDTO dto, HttpServletRequest request) {
        return projectCategoryService.update(id, dto, ApiAuthUtil.getUserId(request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("project:category:delete")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        return projectCategoryService.delete(id, ApiAuthUtil.getUserId(request));
    }
}
