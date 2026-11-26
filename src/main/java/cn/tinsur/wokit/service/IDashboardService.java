package cn.tinsur.wokit.service;

import cn.tinsur.wokit.pojo.vo.DashboardVO;

public interface IDashboardService {

    /**
     * 查询首页数据看板
     *
     * @param weekOffset 任务趋势查看的周偏移：0 本周，-1 上周，依此类推
     */
    DashboardVO get(Long userId, Integer weekOffset);
}
