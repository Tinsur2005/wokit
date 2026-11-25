package cn.tinsur.wokit.service;

import cn.tinsur.wokit.util.Result;

/**
 * 用户工作台数据导出与恢复
 */
public interface IUserDataService {

    /**
     * 导出当前用户全部工作台数据
     */
    byte[] export(Long userId);

    /**
     * 校验并恢复当前用户工作台数据
     */
    Result restore(byte[] content, Long userId);
}
