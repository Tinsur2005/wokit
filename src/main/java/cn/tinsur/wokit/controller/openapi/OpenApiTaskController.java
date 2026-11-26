package cn.tinsur.wokit.controller.openapi;

import cn.tinsur.wokit.exception.ApiException;
import cn.tinsur.wokit.pojo.entity.Task;
import cn.tinsur.wokit.pojo.query.TaskQuery;
import cn.tinsur.wokit.pojo.vo.CalendarDayVO;
import cn.tinsur.wokit.pojo.vo.TaskStatVO;
import cn.tinsur.wokit.security.RequirePermission;
import cn.tinsur.wokit.service.ITaskService;
import cn.tinsur.wokit.util.ApiAuthUtil;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
 * 开放 API：每日任务
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class OpenApiTaskController {

    @Autowired
    private ITaskService taskService;

    @GetMapping
    public Result<IPage<Task>> list(TaskQuery taskQuery, HttpServletRequest request) {
        return Result.ok(taskService.list(taskQuery, ApiAuthUtil.getUserId(request)));
    }

    @GetMapping("/date")
    public Result<List<Task>> listByDate(@RequestParam String date, HttpServletRequest request) {
        return Result.ok(taskService.listByDate(ApiAuthUtil.getUserId(request), date));
    }

    @GetMapping("/calendar")
    public Result<List<CalendarDayVO>> calendar(@RequestParam String month, HttpServletRequest request) {
        return Result.ok(taskService.calendar(ApiAuthUtil.getUserId(request), month));
    }

    @GetMapping("/stat")
    public Result<TaskStatVO> stat(HttpServletRequest request) {
        return Result.ok(taskService.stat(ApiAuthUtil.getUserId(request)));
    }

    @GetMapping("/{id}")
    public Result<Task> getById(@PathVariable Long id, HttpServletRequest request) {
        Task task = taskService.getById(id);
        if (task == null) {
            throw new ApiException(404, "任务不存在或已被删除");
        }
        if (!task.getUserId().equals(ApiAuthUtil.getUserId(request))) {
            throw new ApiException(403, "无权查看该任务");
        }
        return Result.ok(task);
    }

    @PostMapping
    @RequirePermission("task:add")
    public Result add(@RequestBody Task task, HttpServletRequest request) {
        return taskService.add(task, ApiAuthUtil.getUserId(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("task:edit")
    public Result update(@PathVariable Long id, @RequestBody Task task, HttpServletRequest request) {
        return taskService.update(id, task, ApiAuthUtil.getUserId(request));
    }

    @PutMapping("/status/{id}")
    @RequirePermission("task:toggle")
    public Result changeStatus(@PathVariable Long id,
                               @RequestParam Integer status,
                               HttpServletRequest request) {
        return taskService.changeStatus(id, status, ApiAuthUtil.getUserId(request));
    }

    @PutMapping("/priority/{id}")
    @RequirePermission("task:edit")
    public Result changePriority(@PathVariable Long id,
                                 @RequestParam Integer priority,
                                 HttpServletRequest request) {
        return taskService.changePriority(id, priority, ApiAuthUtil.getUserId(request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("task:delete")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        return taskService.delete(id, ApiAuthUtil.getUserId(request));
    }
}
