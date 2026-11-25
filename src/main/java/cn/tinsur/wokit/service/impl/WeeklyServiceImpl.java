package cn.tinsur.wokit.service.impl;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.mapper.ProjectMapper;
import cn.tinsur.wokit.mapper.WeeklyMapper;
import cn.tinsur.wokit.pojo.entity.Project;
import cn.tinsur.wokit.pojo.entity.Weekly;
import cn.tinsur.wokit.service.IWeeklyService;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 周报记录表 服务实现类
 * </p>
 *
 * @author Tinsur
 */
@Service
public class WeeklyServiceImpl extends ServiceImpl<WeeklyMapper, Weekly> implements IWeeklyService {

    @Autowired
    private ProjectMapper projectMapper;

    /**
     * 查询某一周的周报明细（按录入顺序）
     */
    @Override
    public List<Weekly> list(String weekStart, Long userId) {
        LocalDate monday = parseWeekStart(weekStart);
        LambdaQueryWrapper<Weekly> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Weekly::getUserId, userId)
                .eq(Weekly::getWeekStart, toDate(monday))
                .orderByAsc(Weekly::getCreateTime)
                .orderByAsc(Weekly::getId);
        return list(lambdaQueryWrapper);
    }

    /**
     * 新增周报记录
     */
    @Override
    public Result add(Weekly weekly, Long userId) {
        validateWeekly(weekly);
        validateProject(weekly.getProjectId(), userId);
        LocalDate monday = normalizeWeek(weekly.getWeekStart());
        weekly.setId(null);
        weekly.setUserId(userId);
        weekly.setWeekStart(toDate(monday));
        weekly.setWeekEnd(toDate(monday.plusDays(6)));
        save(weekly);
        return Result.ok("周报记录已保存");
    }

    /**
     * 修改周报记录
     */
    @Override
    public Result update(Long id, Weekly weekly, Long userId) {
        Weekly dbWeekly = getOwnWeekly(id, userId);
        validateWeekly(weekly);
        validateProject(weekly.getProjectId(), userId);
        LocalDate monday = weekly.getWeekStart() == null
                ? toLocalDate(dbWeekly.getWeekStart())
                : normalizeWeek(weekly.getWeekStart());
        weekly.setId(dbWeekly.getId());
        weekly.setUserId(userId);
        weekly.setWeekStart(toDate(monday));
        weekly.setWeekEnd(toDate(monday.plusDays(6)));
        updateById(weekly);
        return Result.ok("周报记录已更新");
    }

    /**
     * 删除周报记录（真实删除）
     */
    @Override
    public Result delete(Long id, Long userId) {
        getOwnWeekly(id, userId);
        removeById(id);
        return Result.ok("周报记录已删除");
    }

    /**
     * 校验绑定项目归属
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
     * 校验周报字段：任务内容必填，其余为选填
     */
    private void validateWeekly(Weekly weekly) {
        if (ObjectUtils.isEmpty(weekly.getTaskContent()) || weekly.getTaskContent().trim().isEmpty()) {
            throw new ServiceException("请填写本周完成的任务内容");
        }
        if (weekly.getTaskContent().length() > 200) {
            throw new ServiceException("任务内容不能超过200个字");
        }
        checkLength(weekly.getTaskResult(), 500, "任务结果不能超过500个字");
        checkLength(weekly.getResolvedProblem(), 500, "已解决问题及解决方式不能超过500个字");
        checkLength(weekly.getProblem(), 500, "仍存在的问题不能超过500个字");
        checkLength(weekly.getOutput(), 500, "产出不能超过500个字");
        checkLength(weekly.getExtra(), 500, "其他补充不能超过500个字");

        weekly.setTaskContent(weekly.getTaskContent().trim());
        weekly.setTaskResult(trim(weekly.getTaskResult()));
        weekly.setResolvedProblem(trim(weekly.getResolvedProblem()));
        weekly.setProblem(trim(weekly.getProblem()));
        weekly.setOutput(trim(weekly.getOutput()));
        weekly.setExtra(trim(weekly.getExtra()));
    }

    /**
     * 校验选填字段长度
     */
    private void checkLength(String value, int maxLength, String message) {
        if (!ObjectUtils.isEmpty(value) && value.length() > maxLength) {
            throw new ServiceException(message);
        }
    }

    /**
     * 去掉首尾空格，null 原样返回
     */
    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * 查询周报记录并校验归属
     */
    private Weekly getOwnWeekly(Long id, Long userId) {
        Weekly weekly = getById(id);
        if (weekly == null) {
            throw new ServiceException("周报记录不存在或已被删除");
        }
        if (!weekly.getUserId().equals(userId)) {
            throw new ServiceException("无权操作该周报记录");
        }
        return weekly;
    }

    /**
     * 解析周起始日期，为空时取本周一；传入任意日期都归一到当周周一
     */
    private LocalDate parseWeekStart(String weekStart) {
        if (ObjectUtils.isEmpty(weekStart)) {
            return LocalDate.now().with(DayOfWeek.MONDAY);
        }
        try {
            return LocalDate.parse(weekStart).with(DayOfWeek.MONDAY);
        } catch (DateTimeParseException e) {
            throw new ServiceException("周起始日期格式不正确，应为 yyyy-MM-dd");
        }
    }

    /**
     * Date 归一化到当周周一
     */
    private LocalDate normalizeWeek(Date weekStart) {
        LocalDate day = weekStart == null ? LocalDate.now() : toLocalDate(weekStart);
        return day.with(DayOfWeek.MONDAY);
    }

    /**
     * Date -> LocalDate
     */
    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * LocalDate -> 当天 00:00:00 的 Date
     */
    private Date toDate(LocalDate day) {
        return Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
