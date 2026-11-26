package cn.tinsur.wokit.controller.openapi;

import cn.tinsur.wokit.pojo.vo.TaskCategoryVO;
import cn.tinsur.wokit.service.ITaskCategoryService;
import cn.tinsur.wokit.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 开放 API：项目任务分类只读列表。分类由管理员在网页端统一维护。
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/api/v1/task-categories")
public class OpenApiTaskCategoryController {

    @Autowired
    private ITaskCategoryService taskCategoryService;

    @GetMapping
    public Result<List<TaskCategoryVO>> list() {
        return Result.ok(taskCategoryService.listWithUsage());
    }
}
