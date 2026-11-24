package cn.tinsur.wokit.service;

import cn.tinsur.wokit.pojo.entity.Task;
import cn.tinsur.wokit.pojo.query.TaskQuery;
import cn.tinsur.wokit.pojo.vo.CalendarDayVO;
import cn.tinsur.wokit.pojo.vo.TaskStatVO;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ITaskService extends IService<Task> {

    /**
     * 分页查询任务
     */
    IPage<Task> list(TaskQuery taskQuery, Long userId);

    /**
     * 查询某一天截止的任务列表
     */
    List<Task> listByDate(Long userId, String date);

    /**
     * 查询某个月每天的任务数量标记
     */
    List<CalendarDayVO> calendar(Long userId, String month);

    /**
     * 查询任务统计卡片数据
     */
    TaskStatVO stat(Long userId);

    /**
     * 新增任务
     */
    Result add(Task task, Long userId);

    /**
     * 修改任务
     */
    Result update(Long id, Task task, Long userId);

    /**
     * 切换任务状态
     */
    Result changeStatus(Long id, Integer status, Long userId);

    /**
     * 删除任务（真实删除）
     */
    Result delete(Long id, Long userId);
}
