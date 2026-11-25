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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 每日任务表 服务实现类
 * </p>
 *
 * @author Tinsur
 */
@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements ITaskService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_DONE = 3;
    private static final int PRIORITY_NORMAL = 2;

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

        // 筛选时间区间覆盖当天的任务
        if (!ObjectUtils.isEmpty(taskQuery.getDate())) {
            LocalDate day = parseDate(taskQuery.getDate());
            filterActiveOnDay(lambdaQueryWrapper, day);
        }
        // 未完成排在前面，再按优先级和截止时间排序
        lambdaQueryWrapper.orderByAsc(Task::getStatus)
                .orderByAsc(Task::getPriority)
                .orderByAsc(Task::getDeadline);
        return page(new Page<>(pageNum, pageSize), lambdaQueryWrapper);
    }

    /**
     * 查询某一天时间区间覆盖的任务列表
     */
    @Override
    public List<Task> listByDate(Long userId, String date) {
        LocalDate day = parseDate(date);
        LambdaQueryWrapper<Task> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Task::getUserId, userId);
        filterActiveOnDay(lambdaQueryWrapper, day);
        lambdaQueryWrapper.orderByAsc(Task::getStatus)
                .orderByAsc(Task::getPriority)
                .orderByAsc(Task::getDeadline);
        return list(lambdaQueryWrapper);
    }

    /**
     * 查询某个月每天的任务数量标记（跨天任务在覆盖的每一天都计数）
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

        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        LocalDate nextMonthStart = yearMonth.plusMonths(1).atDay(1);

        // 1.查询与本月有交集的任务，只取开始、截止和状态字段
        LambdaQueryWrapper<Task> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Task::getUserId, userId)
                .ge(Task::getDeadline, startOfDay(monthStart))
                .lt(Task::getStartTime, startOfDay(nextMonthStart))
                .select(Task::getStartTime, Task::getDeadline, Task::getStatus);
        List<Task> taskList = list(lambdaQueryWrapper);

        // 2.把每个任务按实际覆盖日期展开，逐天累计总数和未完成数
        Map<LocalDate, int[]> dailyStats = new HashMap<>();
        for (Task task : taskList) {
            LocalDate start = toLocalDate(task.getStartTime());
            LocalDate end = toLocalDate(task.getDeadline());
            if (start.isBefore(monthStart)) {
                start = monthStart;
            }
            if (end.isAfter(monthEnd)) {
                end = monthEnd;
            }
            for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
                int[] stats = dailyStats.computeIfAbsent(day, key -> new int[2]);
                stats[0]++;
                if (task.getStatus() != null && task.getStatus() != STATUS_DONE) {
                    stats[1]++;
                }
            }
        }

        List<CalendarDayVO> calendarDayVOList = new ArrayList<>();
        dailyStats.forEach((day, stats) -> {
            CalendarDayVO vo = new CalendarDayVO();
            vo.setDate(day.toString());
            vo.setTotal(stats[0]);
            vo.setPending(stats[1]);
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

        // 今日时间区间覆盖的任务总数与待办数
        LambdaQueryWrapper<Task> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(Task::getUserId, userId);
        filterActiveOnDay(todayWrapper, today);
        vo.setTodayTotal(Math.toIntExact(count(todayWrapper)));

        LambdaQueryWrapper<Task> todayPendingWrapper = new LambdaQueryWrapper<>();
        todayPendingWrapper.eq(Task::getUserId, userId)
                .ne(Task::getStatus, STATUS_DONE);
        filterActiveOnDay(todayPendingWrapper, today);
        vo.setTodayPending(Math.toIntExact(count(todayPendingWrapper)));

        // 本周完成数量：按更新时间统计，状态切换为已完成时刷新
        LambdaQueryWrapper<Task> weekWrapper = new LambdaQueryWrapper<>();
        weekWrapper.eq(Task::getUserId, userId)
                .eq(Task::getStatus, STATUS_DONE)
                .ge(Task::getUpdateTime, startOfDay(weekStart))
                .lt(Task::getUpdateTime, startOfDay(nextWeekStart));
        vo.setWeekCompleted(Math.toIntExact(count(weekWrapper)));

        // 本月任务总数与完成数：统计与本月有交集的唯一任务
        LambdaQueryWrapper<Task> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.eq(Task::getUserId, userId)
                .ge(Task::getDeadline, startOfDay(monthStart))
                .lt(Task::getStartTime, startOfDay(nextMonthStart));
        vo.setMonthTotal(Math.toIntExact(count(monthWrapper)));

        LambdaQueryWrapper<Task> monthCompletedWrapper = new LambdaQueryWrapper<>();
        monthCompletedWrapper.eq(Task::getUserId, userId)
                .eq(Task::getStatus, STATUS_DONE)
                .ge(Task::getDeadline, startOfDay(monthStart))
                .lt(Task::getStartTime, startOfDay(nextMonthStart));
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
        if (task.getPriority() == null) {
            task.setPriority(PRIORITY_NORMAL);
        }
        if (task.getStatus() == null) {
            task.setStatus(STATUS_PENDING);
        }
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
        // 未传优先级或状态时保留原值
        if (task.getPriority() == null) {
            task.setPriority(dbTask.getPriority());
        }
        if (task.getStatus() == null) {
            task.setStatus(dbTask.getStatus());
        }
        updateById(task);
        return Result.ok("任务修改成功");
    }

    /**
     * 切换任务状态
     */
    @Override
    public Result changeStatus(Long id, Integer status, Long userId) {
        Task dbTask = getOwnTask(id, userId);
        if (status == null || status < STATUS_PENDING || status > STATUS_DONE) {
            throw new ServiceException("任务状态值不正确");
        }
        Task updateTask = new Task();
        updateTask.setId(dbTask.getId());
        updateTask.setStatus(status);
        updateById(updateTask);
        return Result.ok(switch (status) {
            case 0 -> "任务已恢复为待处理";
            case 1 -> "任务已进入进行中";
            case 2 -> "任务已标记为阻塞中";
            default -> "任务已完成";
        });
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
        if (task.getPriority() != null && (task.getPriority() < 0 || task.getPriority() > 3)) {
            throw new ServiceException("任务优先级值不正确");
        }
        if (task.getStatus() != null && (task.getStatus() < STATUS_PENDING || task.getStatus() > STATUS_DONE)) {
            throw new ServiceException("任务状态值不正确");
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
     * 筛选时间区间覆盖指定日期的任务：
     * 开始时间早于次日零点，且截止时间不早于当天零点。
     */
    private void filterActiveOnDay(LambdaQueryWrapper<Task> wrapper, LocalDate day) {
        wrapper.ge(Task::getDeadline, startOfDay(day))
                .lt(Task::getStartTime, startOfDay(day.plusDays(1)));
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
