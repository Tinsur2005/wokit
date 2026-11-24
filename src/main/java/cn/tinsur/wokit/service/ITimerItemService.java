package cn.tinsur.wokit.service;

import cn.tinsur.wokit.pojo.entity.TimerItem;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ITimerItemService extends IService<TimerItem> {

    /**
     * 查询当前用户的计时列表
     */
    List<TimerItem> list(Long userId);

    /**
     * 新增计时
     */
    Result add(TimerItem timerItem, Long userId);

    /**
     * 修改计时
     */
    Result update(Long id, TimerItem timerItem, Long userId);

    /**
     * 删除计时（真实删除）
     */
    Result delete(Long id, Long userId);
}
