package cn.tinsur.wokit.service;

import cn.tinsur.wokit.pojo.vo.DashboardVO;

public interface IDashboardService {

    /**
     * 查询首页数据看板
     */
    DashboardVO get(Long userId);
}
