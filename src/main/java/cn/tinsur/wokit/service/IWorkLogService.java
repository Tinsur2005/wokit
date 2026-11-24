package cn.tinsur.wokit.service;

import cn.tinsur.wokit.pojo.entity.WorkLog;
import cn.tinsur.wokit.pojo.query.WorkLogQuery;
import cn.tinsur.wokit.pojo.vo.WorkLogStatVO;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IWorkLogService extends IService<WorkLog> {

    /**
     * 分页查询工时记录
     */
    IPage<WorkLog> list(WorkLogQuery workLogQuery, Long userId);

    /**
     * 查询某月工时统计
     */
    WorkLogStatVO stat(Long userId, String month);

    /**
     * 新增工时记录
     */
    Result add(WorkLog workLog, Long userId);

    /**
     * 修改工时记录
     */
    Result update(Long id, WorkLog workLog, Long userId);

    /**
     * 删除工时记录（真实删除）
     */
    Result delete(Long id, Long userId);
}
