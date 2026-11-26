package cn.tinsur.wokit.service.impl;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.mapper.ProjectMapper;
import cn.tinsur.wokit.mapper.TaskCategoryMapper;
import cn.tinsur.wokit.mapper.TaskMapper;
import cn.tinsur.wokit.pojo.entity.Project;
import cn.tinsur.wokit.pojo.entity.Task;
import cn.tinsur.wokit.pojo.entity.TaskCategory;
import cn.tinsur.wokit.pojo.query.TaskQuery;
import cn.tinsur.wokit.pojo.vo.CalendarDayVO;
import cn.tinsur.wokit.pojo.vo.TaskStatVO;
import cn.tinsur.wokit.service.ITaskCategoryService;
import cn.tinsur.wokit.service.ITaskService;
import cn.tinsur.wokit.util.ProjectStatus;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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

    public static final int TASK_KIND_NORMAL = 0;
    public static final int TASK_KIND_PROJECT = 1;

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_DONE = 3;
    private static final int PRIORITY_NORMAL = 2;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private TaskCategoryMapper taskCategoryMapper;

    @Autowired
    private ITaskCategoryService taskCategoryService;

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
                .eq(!ObjectUtils.isEmpty(taskQuery.getStatus()), Task::getStatus, taskQuery.getStatus())
                .eq(!ObjectUtils.isEmpty(taskQuery.getTaskKind()), Task::getTaskKind, taskQuery.getTaskKind())
                .eq(!ObjectUtils.isEmpty(taskQuery.getProjectId()), Task::getProjectId, taskQuery.getProjectId())
                .eq(!ObjectUtils.isEmpty(taskQuery.getTaskCategoryId()),
                        Task::getTaskCategoryId, taskQuery.getTaskCategoryId());

        // 筛选时间区间覆盖当天的任务
        if (!ObjectUtils.isEmpty(taskQuery.getDate())) {
            LocalDate day = parseDate(taskQuery.getDate());
            filterActiveOnDay(lambdaQueryWrapper, day);
        }
        // 按优先级、状态和截止时间排序
        lambdaQueryWrapper.orderByAsc(Task::getPriority)
                .orderByAsc(Task::getStatus)
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
        lambdaQueryWrapper.orderByAsc(Task::getPriority)
                .orderByAsc(Task::getStatus)
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

        // 本周完成数量：按实际完成时间统计
        LambdaQueryWrapper<Task> weekWrapper = new LambdaQueryWrapper<>();
        weekWrapper.eq(Task::getUserId, userId)
                .eq(Task::getStatus, STATUS_DONE)
                .ge(Task::getCompletedAt, startOfDay(weekStart))
                .lt(Task::getCompletedAt, startOfDay(nextWeekStart));
        vo.setWeekCompleted(Math.toIntExact(count(weekWrapper)));

        // 本周任务总数：统计与本周有交集的唯一任务
        LambdaQueryWrapper<Task> weekTotalWrapper = new LambdaQueryWrapper<>();
        weekTotalWrapper.eq(Task::getUserId, userId)
                .ge(Task::getDeadline, startOfDay(weekStart))
                .lt(Task::getStartTime, startOfDay(nextWeekStart));
        vo.setWeekTotal(Math.toIntExact(count(weekTotalWrapper)));

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
     * 新增任务。普通任务不绑定项目；项目任务必须绑定项目和分类。
     */
    @Override
    public Result add(Task task, Long userId) {
        validateTask(task);
        boolean legacyProjectTask = task.getTaskKind() == null && task.getProjectId() != null;
        Integer taskKind = task.getTaskKind() == null
                ? (legacyProjectTask ? TASK_KIND_PROJECT : TASK_KIND_NORMAL)
                : task.getTaskKind();
        if (taskKind != TASK_KIND_NORMAL && taskKind != TASK_KIND_PROJECT) {
            throw new ServiceException("任务类型值不正确");
        }

        if (taskKind == TASK_KIND_NORMAL) {
            if (task.getProjectId() != null || task.getTaskCategoryId() != null) {
                throw new ServiceException("普通任务不能关联项目和任务分类");
            }
        } else {
            if (task.getProjectId() == null) {
                throw new ServiceException("项目任务必须选择项目");
            }
            validateProjectForCreate(task.getProjectId(), userId);
            if (task.getTaskCategoryId() == null) {
                if (!legacyProjectTask) {
                    throw new ServiceException("项目任务必须选择任务分类");
                }
                TaskCategory fallback = taskCategoryService.fallbackCategory();
                if (fallback == null) {
                    throw new ServiceException("暂无可用的任务分类，请先在系统设置中创建");
                }
                task.setTaskCategoryId(fallback.getId());
            }
            validateCategory(task.getTaskCategoryId());
            taskKind = TASK_KIND_PROJECT;
        }

        task.setId(null);
        task.setUserId(userId);
        task.setTaskKind(taskKind);
        if (taskKind == TASK_KIND_NORMAL) {
            task.setProjectId(null);
            task.setTaskCategoryId(null);
        }
        if (task.getPriority() == null) {
            task.setPriority(PRIORITY_NORMAL);
        }
        if (task.getStatus() == null) {
            task.setStatus(STATUS_PENDING);
        }
        task.setCompletedAt(task.getStatus() == STATUS_DONE ? now() : null);
        save(task);
        return Result.ok("任务创建成功");
    }

    /**
     * 修改任务。任务类型、绑定项目和分类创建后不可变更。
     */
    @Override
    public Result update(Long id, Task task, Long userId) {
        Task dbTask = getOwnTask(id, userId);
        validateTask(task);

        Integer incomingKind = task.getTaskKind();
        if (incomingKind != null && !incomingKind.equals(dbTask.getTaskKind())) {
            throw new ServiceException("任务类型创建后不可修改");
        }
        Long incomingProjectId = task.getProjectId();
        if (incomingProjectId != null && !incomingProjectId.equals(dbTask.getProjectId())) {
            throw new ServiceException("项目任务绑定项目创建后不可修改");
        }
        Long incomingCategoryId = task.getTaskCategoryId();
        if (incomingCategoryId != null && !incomingCategoryId.equals(dbTask.getTaskCategoryId())) {
            throw new ServiceException("项目任务分类创建后不可修改");
        }
        if (dbTask.getTaskKind() == TASK_KIND_NORMAL
                && (incomingProjectId != null || incomingCategoryId != null)) {
            throw new ServiceException("普通任务不能关联项目和任务分类");
        }
        if (dbTask.getTaskKind() == TASK_KIND_PROJECT) {
            validateProjectExists(dbTask.getProjectId(), userId);
            validateCategory(dbTask.getTaskCategoryId());
        }

        task.setId(dbTask.getId());
        task.setUserId(userId);
        task.setTaskKind(dbTask.getTaskKind());
        task.setProjectId(dbTask.getProjectId());
        task.setTaskCategoryId(dbTask.getTaskCategoryId());
        if (task.getPriority() == null) {
            task.setPriority(dbTask.getPriority());
        }
        Integer newStatus = task.getStatus() == null ? dbTask.getStatus() : task.getStatus();
        task.setStatus(newStatus);
        if (newStatus == STATUS_DONE) {
            task.setCompletedAt(dbTask.getCompletedAt() == null ? now() : dbTask.getCompletedAt());
        } else {
            task.setCompletedAt(null);
        }
        updateById(task);
        if (newStatus != STATUS_DONE && dbTask.getCompletedAt() != null) {
            update(null, new LambdaUpdateWrapper<Task>()
                    .eq(Task::getId, dbTask.getId())
                    .set(Task::getCompletedAt, null));
        }
        return Result.ok("任务修改成功");
    }

    /**
     * 切换任务状态。完成时记录完成时间，重新打开时清除。
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
        updateTask.setCompletedAt(status == STATUS_DONE
                ? (dbTask.getCompletedAt() == null ? now() : dbTask.getCompletedAt())
                : null);
        updateById(updateTask);
        if (status != STATUS_DONE && dbTask.getCompletedAt() != null) {
            update(null, new LambdaUpdateWrapper<Task>()
                    .eq(Task::getId, dbTask.getId())
                    .set(Task::getCompletedAt, null));
        }
        return Result.ok(switch (status) {
            case 0 -> "任务已恢复为待处理";
            case 1 -> "任务已进入进行中";
            case 2 -> "任务已标记为阻塞中";
            default -> "任务已完成";
        });
    }

    /**
     * 修改任务优先级
     */
    @Override
    public Result changePriority(Long id, Integer priority, Long userId) {
        Task dbTask = getOwnTask(id, userId);
        if (priority == null || priority < 0 || priority > 3) {
            throw new ServiceException("任务优先级值不正确");
        }
        Task updateTask = new Task();
        updateTask.setId(dbTask.getId());
        updateTask.setPriority(priority);
        updateById(updateTask);
        return Result.ok(switch (priority) {
            case 0 -> "任务优先级已设为紧急";
            case 1 -> "任务优先级已设为重要";
            case 2 -> "任务优先级已设为普通";
            default -> "任务优先级已设为低优先级";
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
     * 新增项目任务时校验：项目属于当前用户、未归档且允许继续新增任务。
     */
    private Project validateProjectForCreate(Long projectId, Long userId) {
        Project project = validateProjectExists(projectId, userId);
        if (project.getArchivedAt() != null) {
            throw new ServiceException("项目已归档，请先恢复项目");
        }
        if (!ProjectStatus.canCreateTask(project.getStatus())) {
            if (project.getStatus() != null && project.getStatus() == ProjectStatus.PAUSED) {
                throw new ServiceException("项目已暂停，请先恢复为进行中");
            }
            throw new ServiceException("项目当前状态不允许新增任务");
        }
        return project;
    }

    private Project validateProjectExists(Long projectId, Long userId) {
        if (projectId == null) {
            throw new ServiceException("项目任务必须选择项目");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null || !project.getUserId().equals(userId)) {
            throw new ServiceException("绑定项目不存在或无权使用");
        }
        return project;
    }

    private void validateCategory(Long categoryId) {
        if (categoryId == null) {
            throw new ServiceException("项目任务必须选择任务分类");
        }
        TaskCategory category = taskCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new ServiceException("任务分类不存在或已被删除");
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

    private Date now() {
        return new Date();
    }
}
