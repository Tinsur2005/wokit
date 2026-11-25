package cn.tinsur.wokit.service;

import cn.tinsur.wokit.pojo.dto.SystemConfigDTO;

import java.util.Map;

public interface ISystemConfigService {

    /**
     * 读取全部配置，key 为配置键
     */
    Map<String, String> getAll();

    /**
     * 读取前端需要的系统信息
     */
    SystemConfigDTO getSystemInfo();

    /**
     * 保存系统信息
     */
    void updateSystemInfo(SystemConfigDTO dto);
}
