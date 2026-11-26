package cn.tinsur.wokit.controller.openapi;

import cn.tinsur.wokit.pojo.dto.ProjectDTO;
import cn.tinsur.wokit.pojo.vo.ProjectVO;
import cn.tinsur.wokit.security.RequirePermission;
import cn.tinsur.wokit.service.IProjectService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 开放 API：项目
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/api/v1/projects")
public class OpenApiProjectController {

    @Autowired
    private IProjectService projectService;

    @GetMapping
    public Result<List<ProjectVO>> list(@RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) String keyword,
                                        HttpServletRequest request) {
        return Result.ok(projectService.listByUser(ApiAuthUtil.getUserId(request), categoryId, keyword));
    }

    @PostMapping
    @RequirePermission("project:add")
    public Result add(@RequestBody ProjectDTO dto, HttpServletRequest request) {
        return projectService.add(dto, ApiAuthUtil.getUserId(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("project:edit")
    public Result update(@PathVariable Long id, @RequestBody ProjectDTO dto, HttpServletRequest request) {
        return projectService.update(id, dto, ApiAuthUtil.getUserId(request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("project:delete")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        return projectService.delete(id, ApiAuthUtil.getUserId(request));
    }
}
