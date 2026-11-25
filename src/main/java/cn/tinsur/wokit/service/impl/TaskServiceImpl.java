package cn.tinsur.wokit.service.impl;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.mapper.ProjectMapper;
import cn.tinsur.wokit.mapper.TaskMapper;
import cn.tinsur.wokit.pojo.entity.Project;
import cn.tinsur.wokit.pojo.entity.Task;
import cn.tinsur.wokit.pojo.query.TaskQuery;
import cn.tinsur.wokit.pojo.vo.CalendarDayVO;
import cn.tinsur.wokit.pojo.vo.TaskStatVO;
import cn.tinsur.wokit.service.ITaskService;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 每日任务表 服务实现类
 * </p>
 *
 * @author Tinsur
 */
@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements ITaskService {

    @Autowired
    private ProjectMapper projectMapper;

    /**
     * 分页查询任务
     */
    @Override
    public IPage<Task> list(TaskQuery taskQuery, Long userId) {
        Integer pageNum = ObjectUtils.isEmpty(taskQuery.getPage()) ? 1 : taskQuery.getPage();
        Integer pageSize = ObjectUtils.isEmpty(taskQuery.getLimit()) ? 10 : taskQuery.getLimit();

        LambdaQueryWrapper<Task> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(Task::getUserId, userId)
                .and(!ObjectUtils.isEmpty(taskQuery.getKeyword()), wrapper -> wrapper
                        .like(Task::getTitle, taskQuery.getKeyword())
                        .or()
                        .like(Task::getDescription, taskQuery.getKeyword()))
                .eq(!ObjectUtils.isEmpty(taskQuery.getStatus()), Task::getStatus, taskQuery.getStatus());

        // 按截止日期筛选
        if (!ObjectUtils.isEmpty(taskQuery.getDate())) {
            LocalDate day = parseDate(taskQuery.getDate());
            lambdaQueryWrapper.ge(Task::getDeadline, startOfDay(day))
                    .lt(Task::getDeadline, startOfDay(day.plusDays(1)));
        }
        // 待完成排在前面，再按截止时间升序
        lambdaQueryWrapper.orderByAsc(Task::getStatus).orderByAsc(Task::getDeadline);
        return page(new Page<>(pageNum, pageSize), lambdaQueryWrapper);
    }

    /**
     * 查询某一天截止的任务列表
     */
    @Override
    public List<Task> listByDate(Long userId, String date) {
        LocalDate day = parseDate(date);
        LambdaQueryWrapper<Task> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Task::getUserId, userId)
                .ge(Task::getDeadline, startOfDay(day))
                .lt(Task::getDeadline, startOfDay(day.plusDays(1)))
                .orderByAsc(Task::getStatus)
                .orderByAsc(Task::getDeadline);
        return list(lambdaQueryWrapper);
    }

    /**
     * 查询某个月每天的任务数量标记
     */
    @Override
    public List<CalendarDayVO> calendar(Long userId, String month) {
        if (ObjectUtils.isEmpty(month)) {
            throw new ServiceException("请传入月份");
        }
        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new ServiceException("月份格式不正确，应为 yyyy-MM");
        }

        // 1.只查这个月需要的截止时间和状态两个字段
        LambdaQueryWrapper<Task> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Task::getUserId, userId)
                .ge(Task::getDeadline, startOfDay(yearMonth.atDay(1)))
                .lt(Task::getDeadline, startOfDay(yearMonth.plusMonths(1).atDay(1)))
                .select(Task::getDeadline, Task::getStatus);
        List<Task> taskList = list(lambdaQueryWrapper);

        // 2.按截止日期分组，统计每天的总数和未完成数
        Map<LocalDate, List<Task>> groupByDate = taskList.stream()
                .collect(Collectors.groupingBy(task -> toLocalDate(task.getDeadline())));

        List<CalendarDayVO> calendarDayVOList = new ArrayList<>();
        groupByDate.forEach((day, tasks) -> {
            CalendarDayVO vo = new CalendarDayVO();
            vo.setDate(day.toString());
            vo.setTotal(tasks.size());
            vo.setPending((int) tasks.stream().filter(task -> task.getStatus() != null && task.getStatus() == 0).count());
            calendarDayVOList.add(vo);
        });
        calendarDayVOList.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        return calendarDayVOList;
    }

    /**
     * 查询任务统计卡片数据
     */
    @Override
    public TaskStatVO stat(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate nextMonthStart = monthStart.plusMonths(1);
        // 本周从周一开始
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate nextWeekStart = weekStart.plusWeeks(1);

        TaskStatVO vo = new TaskStatVO();

        // 今日截止任务总数与待办数
        LambdaQueryWrapper<Task> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(Task::getUserId, userId)
                .ge(Task::getDeadline, startOfDay(today))
                .lt(Task::getDeadline, startOfDay(today.plusDays(1)));
        vo.setTodayTotal(Math.toIntExact(count(todayWrapper)));

        LambdaQueryWrapper<Task> todayPendingWrapper = new LambdaQueryWrapper<>();
        todayPendingWrapper.eq(Task::getUserId, userId)
                .eq(Task::getStatus, 0)
                .ge(Task::getDeadline, startOfDay(today))
                .lt(Task::getDeadline, startOfDay(today.plusDays(1)));
        vo.setTodayPending(Math.toIntExact(count(todayPendingWrapper)));

        // 本周完成数量：按更新时间统计，状态切换为已完成时刷新
        LambdaQueryWrapper<Task> weekWrapper = new LambdaQueryWrapper<>();
        weekWrapper.eq(Task::getUserId, userId)
                .eq(Task::getStatus, 1)
                .ge(Task::getUpdateTime, startOfDay(weekStart))
                .lt(Task::getUpdateTime, startOfDay(nextWeekStart));
        vo.setWeekCompleted(Math.toIntExact(count(weekWrapper)));

        // 本月任务总数与完成数
        LambdaQueryWrapper<Task> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.eq(Task::getUserId, userId)
                .ge(Task::getDeadline, startOfDay(monthStart))
                .lt(Task::getDeadline, startOfDay(nextMonthStart));
        vo.setMonthTotal(Math.toIntExact(count(monthWrapper)));

        LambdaQueryWrapper<Task> monthCompletedWrapper = new LambdaQueryWrapper<>();
        monthCompletedWrapper.eq(Task::getUserId, userId)
                .eq(Task::getStatus, 1)
                .ge(Task::getDeadline, startOfDay(monthStart))
                .lt(Task::getDeadline, startOfDay(nextMonthStart));
        vo.setMonthCompleted(Math.toIntExact(count(monthCompletedWrapper)));

        return vo;
    }

    /**
     * 新增任务
     */
    @Override
    public Result add(Task task, Long userId) {
        validateTask(task);
        validateProject(task.getProjectId(), userId);
        task.setId(null);
        task.setUserId(userId);
        task.setStatus(0);
        save(task);
        return Result.ok("任务创建成功");
    }

    /**
     * 修改任务
     */
    @Override
    public Result update(Long id, Task task, Long userId) {
        // 1.只允许修改自己的任务
        Task dbTask = getOwnTask(id, userId);
        // 2.校验字段
        validateTask(task);
        validateProject(task.getProjectId(), userId);
        task.setId(dbTask.getId());
        task.setUserId(userId);
        // 状态不允许通过编辑接口修改，走单独的状态切换接口
        task.setStatus(null);
        updateById(task);
        return Result.ok("任务修改成功");
    }

    /**
     * 切换任务状态
     */
    @Override
    public Result changeStatus(Long id, Integer status, Long userId) {
        Task dbTask = getOwnTask(id, userId);
        if (status == null || (status != 0 && status != 1)) {
            throw new ServiceException("任务状态值不正确");
        }
        Task updateTask = new Task();
        updateTask.setId(dbTask.getId());
        updateTask.setStatus(status);
        updateById(updateTask);
        return Result.ok(status == 1 ? "任务已完成" : "任务已恢复为待完成");
    }

    /**
     * 删除任务（真实删除）
     */
    @Override
    public Result delete(Long id, Long userId) {
        getOwnTask(id, userId);
        removeById(id);
        return Result.ok("任务已删除");
    }

    /**
     * 校验绑定项目是否存在且属于当前用户；为空表示不绑定
     */
    private void validateProject(Long projectId, Long userId) {
        if (projectId == null) {
            return;
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null || !project.getUserId().equals(userId)) {
            throw new ServiceException("绑定项目不存在或无权使用");
        }
    }

    /**
     * 校验任务字段：标题必填，截止时间不能早于开始时间
     */
    private void validateTask(Task task) {
        if (ObjectUtils.isEmpty(task.getTitle()) || task.getTitle().trim().isEmpty()) {
            throw new ServiceException("请填写任务标题");
        }
        if (task.getStartTime() == null || task.getDeadline() == null) {
            throw new ServiceException("请选择开始时间和截止时间");
        }
        if (task.getDeadline().before(task.getStartTime())) {
            throw new ServiceException("截止时间不能早于开始时间");
        }
        if (task.getTitle().trim().length() > 100) {
            throw new ServiceException("任务标题不能超过100个字");
        }
        if (!ObjectUtils.isEmpty(task.getDescription()) && task.getDescription().length() > 500) {
            throw new ServiceException("任务描述不能超过500个字");
        }
        task.setTitle(task.getTitle().trim());
    }

    /**
     * 查询任务并校验归属，避免越权修改他人的任务
     */
    private Task getOwnTask(Long id, Long userId) {
        Task task = getById(id);
        if (task == null) {
            throw new ServiceException("任务不存在或已被删除");
        }
        if (!task.getUserId().equals(userId)) {
            throw new ServiceException("无权操作该任务");
        }
        return task;
    }

    /**
     * 解析 yyyy-MM-dd，为空时取今天
     */
    private LocalDate parseDate(String date) {
        if (ObjectUtils.isEmpty(date)) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new ServiceException("日期格式不正确，应为 yyyy-MM-dd");
        }
    }

    /**
     * LocalDate -> 当天 00:00:00 的 Date
     */
    private Date startOfDay(LocalDate day) {
        return Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Date -> LocalDate
     */
    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
