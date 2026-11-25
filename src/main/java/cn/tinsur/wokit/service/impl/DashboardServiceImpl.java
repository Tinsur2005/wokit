package cn.tinsur.wokit.service.impl;

import cn.tinsur.wokit.mapper.ProjectMapper;
import cn.tinsur.wokit.mapper.TaskMapper;
import cn.tinsur.wokit.mapper.TimerItemMapper;
import cn.tinsur.wokit.mapper.WeeklyMapper;
import cn.tinsur.wokit.mapper.WorkLogMapper;
import cn.tinsur.wokit.pojo.entity.Project;
import cn.tinsur.wokit.pojo.entity.Task;
import cn.tinsur.wokit.pojo.entity.TimerItem;
import cn.tinsur.wokit.pojo.entity.Weekly;
import cn.tinsur.wokit.pojo.entity.WorkLog;
import cn.tinsur.wokit.pojo.vo.DashboardVO;
import cn.tinsur.wokit.pojo.vo.ProjectHourVO;
import cn.tinsur.wokit.pojo.vo.TrendPointVO;
import cn.tinsur.wokit.pojo.vo.WeeklyTrendVO;
import cn.tinsur.wokit.service.IDashboardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 首页数据看板 服务实现类
 * </p>
 *
 * @author Tinsur
 */
@Service
public class DashboardServiceImpl implements IDashboardService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter SHORT_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final String MODE_COUNTDOWN_DAY = "countdown_day";
    private static final String MODE_PRECISE = "precise";

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private WorkLogMapper workLogMapper;

    @Autowired
    private WeeklyMapper weeklyMapper;

    @Autowired
    private TimerItemMapper timerItemMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Override
    public DashboardVO get(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate todayStart = today;
        LocalDate tomorrow = today.plusDays(1);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate nextWeekStart = weekStart.plusWeeks(1);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate nextMonthStart = monthStart.plusMonths(1);

        DashboardVO dashboardVO = new DashboardVO();
        dashboardVO.setTodayTotal(countTask(userId, null, todayStart, tomorrow));
        dashboardVO.setTodayPending(countTask(userId, 0, todayStart, tomorrow));
        dashboardVO.setWeekCompleted(countCompletedTask(userId, weekStart, nextWeekStart));
        dashboardVO.setMonthHours(sumMonthHours(userId, monthStart, nextMonthStart));
        dashboardVO.setRunningTimers(countRunningTimers(userId, today));
        dashboardVO.setTaskTrend(buildTaskTrend(userId, today));
        dashboardVO.setProjectHours(buildProjectHours(userId, monthStart, nextMonthStart));
        dashboardVO.setWeeklyTrend(buildWeeklyTrend(userId, weekStart));
        return dashboardVO;
    }

    /**
     * 统计某日期区间内截止的任务数
     */
    private Integer countTask(Long userId, Integer status, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .ge(Task::getDeadline, startOfDay(start))
                .lt(Task::getDeadline, startOfDay(end));
        if (status != null) {
            wrapper.eq(Task::getStatus, status);
        }
        return taskMapper.selectCount(wrapper).intValue();
    }

    /**
     * 统计某日期区间内完成的任务数
     */
    private Integer countCompletedTask(Long userId, LocalDate start, LocalDate end) {
        return taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .eq(Task::getStatus, 1)
                .ge(Task::getUpdateTime, startOfDay(start))
                .lt(Task::getUpdateTime, startOfDay(end))).intValue();
    }

    /**
     * 汇总本月工时
     */
    private BigDecimal sumMonthHours(Long userId, LocalDate start, LocalDate end) {
        List<WorkLog> workLogs = workLogMapper.selectList(new LambdaQueryWrapper<WorkLog>()
                .eq(WorkLog::getUserId, userId)
                .ge(WorkLog::getWorkDate, startOfDay(start))
                .lt(WorkLog::getWorkDate, startOfDay(end)));
        return workLogs.stream()
                .map(WorkLog::getHours)
                .filter(hours -> hours != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * 统计进行中的计时：未到期的倒数日 + 未到点的精准倒计时
     */
    private Integer countRunningTimers(Long userId, LocalDate today) {
        List<TimerItem> timerItems = timerItemMapper.selectList(new LambdaQueryWrapper<TimerItem>()
                .eq(TimerItem::getUserId, userId));
        Date now = new Date();
        int running = 0;
        for (TimerItem timerItem : timerItems) {
            if (MODE_COUNTDOWN_DAY.equals(timerItem.getMode())
                    && !toLocalDate(timerItem.getTargetTime()).isBefore(today)) {
                running++;
            } else if (MODE_PRECISE.equals(timerItem.getMode())
                    && timerItem.getTargetTime().after(now)) {
                running++;
            }
        }
        return running;
    }

    /**
     * 近 7 天任务新增与完成趋势
     */
    private List<TrendPointVO> buildTaskTrend(Long userId, LocalDate today) {
        LocalDate start = today.minusDays(6);
        LocalDate end = today.plusDays(1);

        List<Task> createdTasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .ge(Task::getCreateTime, startOfDay(start))
                .lt(Task::getCreateTime, startOfDay(end)));
        List<Task> completedTasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .eq(Task::getStatus, 1)
                .ge(Task::getUpdateTime, startOfDay(start))
                .lt(Task::getUpdateTime, startOfDay(end)));

        Map<LocalDate, Long> createdMap = createdTasks.stream()
                .collect(Collectors.groupingBy(task -> toLocalDate(task.getCreateTime()), Collectors.counting()));
        Map<LocalDate, Long> completedMap = completedTasks.stream()
                .collect(Collectors.groupingBy(task -> toLocalDate(task.getUpdateTime()), Collectors.counting()));

        List<TrendPointVO> points = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = start.plusDays(i);
            TrendPointVO point = new TrendPointVO();
            point.setDate(day.format(DATE_FORMATTER));
            point.setLabel(day.format(SHORT_FORMATTER));
            point.setCreated(createdMap.getOrDefault(day, 0L).intValue());
            point.setCompleted(completedMap.getOrDefault(day, 0L).intValue());
            points.add(point);
        }
        return points;
    }

    /**
     * 本月项目工时分布，最多保留 6 项，其余合并为「其他」
     */
    private List<ProjectHourVO> buildProjectHours(Long userId, LocalDate start, LocalDate end) {
        List<WorkLog> workLogs = workLogMapper.selectList(new LambdaQueryWrapper<WorkLog>()
                .eq(WorkLog::getUserId, userId)
                .ge(WorkLog::getWorkDate, startOfDay(start))
                .lt(WorkLog::getWorkDate, startOfDay(end)));

        List<Long> projectIds = workLogs.stream()
                .map(WorkLog::getProjectId)
                .filter(projectId -> projectId != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> projectNameMap = projectIds.isEmpty()
                ? Map.of()
                : projectMapper.selectList(new LambdaQueryWrapper<Project>()
                        .eq(Project::getUserId, userId)
                        .in(Project::getId, projectIds))
                .stream()
                .collect(Collectors.toMap(Project::getId, Project::getName));

        Map<String, BigDecimal> projectMap = new LinkedHashMap<>();
        for (WorkLog workLog : workLogs) {
            if (workLog.getHours() == null) {
                continue;
            }
            String boundProjectName = workLog.getProjectId() == null
                    ? null : projectNameMap.get(workLog.getProjectId());
            String project = boundProjectName != null && !boundProjectName.trim().isEmpty()
                    ? boundProjectName.trim()
                    : (workLog.getProject() == null || workLog.getProject().trim().isEmpty()
                    ? "未命名事项" : workLog.getProject().trim());
            projectMap.merge(project, workLog.getHours(), BigDecimal::add);
        }

        List<ProjectHourVO> projectHours = projectMap.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(entry -> {
                    ProjectHourVO projectHourVO = new ProjectHourVO();
                    projectHourVO.setProject(entry.getKey());
                    projectHourVO.setHours(entry.getValue().setScale(1, RoundingMode.HALF_UP));
                    return projectHourVO;
                })
                .collect(Collectors.toList());

        if (projectHours.size() <= 6) {
            return projectHours;
        }
        List<ProjectHourVO> result = new ArrayList<>(projectHours.subList(0, 5));
        BigDecimal otherHours = projectHours.subList(5, projectHours.size()).stream()
                .map(ProjectHourVO::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ProjectHourVO other = new ProjectHourVO();
        other.setProject("其他");
        other.setHours(otherHours.setScale(1, RoundingMode.HALF_UP));
        result.add(other);
        return result;
    }

    /**
     * 近 4 周周报记录数量
     */
    private List<WeeklyTrendVO> buildWeeklyTrend(Long userId, LocalDate currentWeekStart) {
        LocalDate start = currentWeekStart.minusWeeks(3);
        List<Weekly> weeklies = weeklyMapper.selectList(new LambdaQueryWrapper<Weekly>()
                .eq(Weekly::getUserId, userId)
                .ge(Weekly::getWeekStart, startOfDay(start))
                .le(Weekly::getWeekStart, startOfDay(currentWeekStart)));

        Map<LocalDate, Long> weeklyMap = weeklies.stream()
                .collect(Collectors.groupingBy(weekly -> toLocalDate(weekly.getWeekStart()), Collectors.counting()));

        List<WeeklyTrendVO> points = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            LocalDate weekStart = start.plusWeeks(i);
            WeeklyTrendVO point = new WeeklyTrendVO();
            point.setWeekStart(weekStart.format(DATE_FORMATTER));
            point.setLabel(weekStart.format(SHORT_FORMATTER));
            point.setCount(weeklyMap.getOrDefault(weekStart, 0L).intValue());
            points.add(point);
        }
        return points;
    }

    /**
     * LocalDate -> 当天 00:00:00
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
