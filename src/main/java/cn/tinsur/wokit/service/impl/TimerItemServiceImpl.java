package cn.tinsur.wokit.service.impl;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.mapper.TimerItemMapper;
import cn.tinsur.wokit.pojo.entity.TimerItem;
import cn.tinsur.wokit.service.ITimerItemService;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 计时表 服务实现类
 * </p>
 *
 * @author Tinsur
 */
@Service
public class TimerItemServiceImpl extends ServiceImpl<TimerItemMapper, TimerItem> implements ITimerItemService {

    private static final String MODE_COUNTDOWN_DAY = "countdown_day";
    private static final String MODE_COUNTUP_DAY = "countup_day";
    private static final String MODE_PRECISE = "precise";

    /**
     * 查询当前用户的计时列表，已过期的倒数日自动转为正数日
     */
    @Override
    public List<TimerItem> list(Long userId) {
        LambdaQueryWrapper<TimerItem> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(TimerItem::getUserId, userId)
                .orderByAsc(TimerItem::getCreateTime)
                .orderByAsc(TimerItem::getId);
        List<TimerItem> timerItems = list(lambdaQueryWrapper);

        LocalDate today = LocalDate.now();
        timerItems.forEach(timerItem -> {
            if (MODE_COUNTDOWN_DAY.equals(timerItem.getMode())
                    && toLocalDate(timerItem.getTargetTime()).isBefore(today)) {
                TimerItem updateItem = new TimerItem();
                updateItem.setId(timerItem.getId());
                updateItem.setMode(MODE_COUNTUP_DAY);
                updateById(updateItem);
                timerItem.setMode(MODE_COUNTUP_DAY);
            }
        });
        return timerItems;
    }

    /**
     * 新增计时
     */
    @Override
    public Result add(TimerItem timerItem, Long userId) {
        validateTimer(timerItem, null);
        timerItem.setId(null);
        timerItem.setUserId(userId);
        save(timerItem);
        return Result.ok("计时已创建");
    }

    /**
     * 修改计时
     */
    @Override
    public Result update(Long id, TimerItem timerItem, Long userId) {
        TimerItem dbTimer = getOwnTimer(id, userId);
        validateTimer(timerItem, dbTimer.getTargetTime());
        timerItem.setId(id);
        timerItem.setUserId(userId);
        updateById(timerItem);
        return Result.ok("计时已更新");
    }

    /**
     * 删除计时（真实删除）
     */
    @Override
    public Result delete(Long id, Long userId) {
        getOwnTimer(id, userId);
        removeById(id);
        return Result.ok("计时已删除");
    }

    /**
     * 校验模式与目标时间
     */
    private void validateTimer(TimerItem timerItem, Date existingTarget) {
        if (ObjectUtils.isEmpty(timerItem.getName()) || timerItem.getName().trim().isEmpty()) {
            throw new ServiceException("请填写计时名称");
        }
        if (timerItem.getName().trim().length() > 30) {
            throw new ServiceException("计时名称不能超过30个字");
        }
        if (timerItem.getTargetTime() == null) {
            throw new ServiceException("请选择目标时间");
        }

        String mode = timerItem.getMode();
        LocalDate today = LocalDate.now();
        if (MODE_COUNTDOWN_DAY.equals(mode)) {
            LocalDate targetDay = toLocalDate(timerItem.getTargetTime());
            if (targetDay.isBefore(today)) {
                throw new ServiceException("倒数日的目标日期不能早于今天");
            }
            timerItem.setTargetTime(startOfDay(targetDay));
        } else if (MODE_COUNTUP_DAY.equals(mode)) {
            LocalDate targetDay = toLocalDate(timerItem.getTargetTime());
            if (targetDay.isAfter(today)) {
                throw new ServiceException("正数日的目标日期不能晚于今天");
            }
            timerItem.setTargetTime(startOfDay(targetDay));
        } else if (MODE_PRECISE.equals(mode)) {
            Date targetTime = toMinute(timerItem.getTargetTime());
            boolean unchanged = existingTarget != null
                    && toMinute(existingTarget).getTime() == targetTime.getTime();
            if (!targetTime.after(new Date()) && !unchanged) {
                throw new ServiceException("精准倒计时的目标时间需晚于当前时间");
            }
            timerItem.setTargetTime(targetTime);
        } else {
            throw new ServiceException("计时模式不正确");
        }
        timerItem.setName(timerItem.getName().trim());
    }

    /**
     * 查询计时并校验归属
     */
    private TimerItem getOwnTimer(Long id, Long userId) {
        TimerItem timerItem = getById(id);
        if (timerItem == null) {
            throw new ServiceException("计时不存在或已被删除");
        }
        if (!timerItem.getUserId().equals(userId)) {
            throw new ServiceException("无权操作该计时");
        }
        return timerItem;
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
    private Date startOfDay(LocalDate day) {
        return Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 精确到分钟，秒和毫秒归零
     */
    private Date toMinute(Date date) {
        return Date.from(date.toInstant().atZone(ZoneId.systemDefault())
                .withSecond(0)
                .withNano(0)
                .toInstant());
    }
}
