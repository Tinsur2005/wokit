package cn.tinsur.wokit.service.impl;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.mapper.WorkLogMapper;
import cn.tinsur.wokit.pojo.entity.WorkLog;
import cn.tinsur.wokit.pojo.query.WorkLogQuery;
import cn.tinsur.wokit.pojo.vo.WorkLogStatVO;
import cn.tinsur.wokit.service.IWorkLogService;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 工时登记表 服务实现类
 * </p>
 *
 * @author Tinsur
 */
@Service
public class WorkLogServiceImpl extends ServiceImpl<WorkLogMapper, WorkLog> implements IWorkLogService {

    /**
     * 分页查询工时记录
     */
    @Override
    public IPage<WorkLog> list(WorkLogQuery workLogQuery, Long userId) {
        Integer pageNum = ObjectUtils.isEmpty(workLogQuery.getPage()) ? 1 : workLogQuery.getPage();
        Integer pageSize = ObjectUtils.isEmpty(workLogQuery.getLimit()) ? 10 : workLogQuery.getLimit();

        LambdaQueryWrapper<WorkLog> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(WorkLog::getUserId, userId);
        if (!ObjectUtils.isEmpty(workLogQuery.getMonth())) {
            YearMonth yearMonth = parseMonth(workLogQuery.getMonth());
            lambdaQueryWrapper.ge(WorkLog::getWorkDate, toDate(yearMonth.atDay(1)))
                    .lt(WorkLog::getWorkDate, toDate(yearMonth.plusMonths(1).atDay(1)));
        }
        lambdaQueryWrapper.and(!ObjectUtils.isEmpty(workLogQuery.getKeyword()), wrapper -> wrapper
                        .like(WorkLog::getProject, workLogQuery.getKeyword())
                        .or()
                        .like(WorkLog::getContent, workLogQuery.getKeyword()))
                .orderByDesc(WorkLog::getWorkDate)
                .orderByDesc(WorkLog::getCreateTime);
        return page(new Page<>(pageNum, pageSize), lambdaQueryWrapper);
    }

    /**
     * 查询某月工时统计
     */
    @Override
    public WorkLogStatVO stat(Long userId, String month) {
        YearMonth yearMonth = parseMonth(month);
        LambdaQueryWrapper<WorkLog> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(WorkLog::getUserId, userId)
                .ge(WorkLog::getWorkDate, toDate(yearMonth.atDay(1)))
                .lt(WorkLog::getWorkDate, toDate(yearMonth.plusMonths(1).atDay(1)));
        List<WorkLog> workLogList = list(lambdaQueryWrapper);

        WorkLogStatVO vo = new WorkLogStatVO();
        BigDecimal total = workLogList.stream()
                .map(WorkLog::getHours)
                .filter(hours -> hours != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Set<String> daySet = new HashSet<>();
        workLogList.forEach(workLog -> {
            if (workLog.getWorkDate() != null) {
                daySet.add(workLog.getWorkDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate().toString());
            }
        });

        vo.setMonthHours(total.setScale(1, RoundingMode.HALF_UP));
        vo.setDays(daySet.size());
        vo.setRecords(workLogList.size());
        vo.setAvgHours(daySet.isEmpty()
                ? BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)
                : total.divide(new BigDecimal(daySet.size()), 1, RoundingMode.HALF_UP));
        return vo;
    }

    /**
     * 新增工时记录
     */
    @Override
    public Result add(WorkLog workLog, Long userId) {
        validateWorkLog(workLog);
        workLog.setId(null);
        workLog.setUserId(userId);
        save(workLog);
        return Result.ok("工时已登记");
    }

    /**
     * 修改工时记录
     */
    @Override
    public Result update(Long id, WorkLog workLog, Long userId) {
        WorkLog dbWorkLog = getOwnWorkLog(id, userId);
        validateWorkLog(workLog);
        workLog.setId(dbWorkLog.getId());
        workLog.setUserId(userId);
        updateById(workLog);
        return Result.ok("工时已更新");
    }

    /**
     * 删除工时记录（真实删除）
     */
    @Override
    public Result delete(Long id, Long userId) {
        getOwnWorkLog(id, userId);
        removeById(id);
        return Result.ok("工时记录已删除");
    }

    /**
     * 校验工时字段：项目必填，日期必填，工时为 0-24 之间
     */
    private void validateWorkLog(WorkLog workLog) {
        if (ObjectUtils.isEmpty(workLog.getProject()) || workLog.getProject().trim().isEmpty()) {
            throw new ServiceException("请填写项目或事项名称");
        }
        if (workLog.getWorkDate() == null) {
            throw new ServiceException("请选择工作日期");
        }
        if (workLog.getHours() == null) {
            throw new ServiceException("请填写工时");
        }
        if (workLog.getHours().compareTo(BigDecimal.ZERO) <= 0
                || workLog.getHours().compareTo(new BigDecimal("24")) > 0) {
            throw new ServiceException("工时需大于0且不超过24小时");
        }
        if (workLog.getProject().length() > 100) {
            throw new ServiceException("项目名称不能超过100个字");
        }
        if (!ObjectUtils.isEmpty(workLog.getContent()) && workLog.getContent().length() > 500) {
            throw new ServiceException("工作内容不能超过500个字");
        }
        workLog.setProject(workLog.getProject().trim());
    }

    /**
     * 查询工时记录并校验归属
     */
    private WorkLog getOwnWorkLog(Long id, Long userId) {
        WorkLog workLog = getById(id);
        if (workLog == null) {
            throw new ServiceException("工时记录不存在或已被删除");
        }
        if (!workLog.getUserId().equals(userId)) {
            throw new ServiceException("无权操作该工时记录");
        }
        return workLog;
    }

    /**
     * 解析月份，为空时取当前月
     */
    private YearMonth parseMonth(String month) {
        if (ObjectUtils.isEmpty(month)) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new ServiceException("月份格式不正确，应为 yyyy-MM");
        }
    }

    /**
     * LocalDate -> 当天 00:00:00 的 Date
     */
    private Date toDate(LocalDate day) {
        return Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
