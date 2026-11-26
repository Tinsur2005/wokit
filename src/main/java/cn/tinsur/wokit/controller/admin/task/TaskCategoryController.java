package cn.tinsur.wokit.controller.admin.task;

import cn.tinsur.wokit.pojo.entity.TaskCategory;
import cn.tinsur.wokit.pojo.vo.TaskCategoryVO;
import cn.tinsur.wokit.service.ITaskCategoryService;
import cn.tinsur.wokit.util.Result;
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
 * 项目任务分类管理
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/admin/task-categories")
public class TaskCategoryController {

    @Autowired
    private ITaskCategoryService taskCategoryService;

    @GetMapping
    public Result<List<TaskCategoryVO>> list() {
        return Result.ok(taskCategoryService.listWithUsage());
    }

    @PostMapping
    public Result add(@RequestBody TaskCategory category) {
        return taskCategoryService.add(category);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody TaskCategory category) {
        return taskCategoryService.update(id, category);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        return taskCategoryService.delete(id);
    }
}
