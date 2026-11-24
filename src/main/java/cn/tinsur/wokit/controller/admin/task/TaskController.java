package cn.tinsur.wokit.controller.admin.task;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.pojo.entity.Task;
import cn.tinsur.wokit.pojo.query.TaskQuery;
import cn.tinsur.wokit.pojo.vo.CalendarDayVO;
import cn.tinsur.wokit.pojo.vo.TaskStatVO;
import cn.tinsur.wokit.service.ITaskService;
import cn.tinsur.wokit.util.AuthUtil;
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
 * <p>
 * 每日任务 前端控制器
 * </p>
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/admin/tasks")
public class TaskController {

    @Autowired
    private ITaskService taskService;

    /**
     * 分页查询任务
     * GET /admin/tasks?page=1&limit=10&keyword=xxx&status=0&date=2026-11-24
     */
    @GetMapping
    public Result<IPage<Task>> list(TaskQuery taskQuery, HttpServletRequest request) {
        return Result.ok(taskService.list(taskQuery, AuthUtil.getUserId(request)));
    }

    /**
     * 查询某一天截止的任务列表
     * GET /admin/tasks/date?date=2026-11-24
     */
    @GetMapping("/date")
    public Result<List<Task>> listByDate(@RequestParam String date, HttpServletRequest request) {
        return Result.ok(taskService.listByDate(AuthUtil.getUserId(request), date));
    }

    /**
     * 查询某个月每天的任务数量标记
     * GET /admin/tasks/calendar?month=2026-11
     */
    @GetMapping("/calendar")
    public Result<List<CalendarDayVO>> calendar(@RequestParam String month, HttpServletRequest request) {
        return Result.ok(taskService.calendar(AuthUtil.getUserId(request), month));
    }

    /**
     * 查询任务统计卡片数据
     * GET /admin/tasks/stat
     */
    @GetMapping("/stat")
    public Result<TaskStatVO> stat(HttpServletRequest request) {
        return Result.ok(taskService.stat(AuthUtil.getUserId(request)));
    }

    /**
     * 根据ID查询任务
     * GET /admin/tasks/1
     */
    @GetMapping("/{id}")
    public Result<Task> getById(@PathVariable Long id, HttpServletRequest request) {
        Task task = taskService.getById(id);
        if (task == null) {
            throw new ServiceException("任务不存在或已被删除");
        }
        if (!task.getUserId().equals(AuthUtil.getUserId(request))) {
            throw new ServiceException("无权查看该任务");
        }
        return Result.ok(task);
    }

    /**
     * 新增任务
     * POST /admin/tasks
     */
    @PostMapping
    public Result add(@RequestBody Task task, HttpServletRequest request) {
        return taskService.add(task, AuthUtil.getUserId(request));
    }

    /**
     * 修改任务
     * PUT /admin/tasks/1
     */
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody Task task, HttpServletRequest request) {
        return taskService.update(id, task, AuthUtil.getUserId(request));
    }

    /**
     * 切换任务状态
     * PUT /admin/tasks/status/1?status=1
     */
    @PutMapping("/status/{id}")
    public Result changeStatus(@PathVariable Long id,
                               @RequestParam Integer status,
                               HttpServletRequest request) {
        return taskService.changeStatus(id, status, AuthUtil.getUserId(request));
    }

    /**
     * 删除任务（真实删除）
     * DELETE /admin/tasks/1
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        return taskService.delete(id, AuthUtil.getUserId(request));
    }
}
