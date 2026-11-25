package cn.tinsur.wokit.service.impl;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.mapper.SystemConfigMapper;
import cn.tinsur.wokit.pojo.dto.SystemConfigDTO;
import cn.tinsur.wokit.pojo.entity.SystemConfig;
import cn.tinsur.wokit.service.ISystemConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统配置服务
 *
 * @author Tinsur
 */
@Service
public class SystemConfigServiceImpl implements ISystemConfigService {

    private static final String KEY_SYSTEM_NAME = "system_name";
    private static final String KEY_SYSTEM_SUB_NAME = "system_sub_name";
    private static final String KEY_BROWSER_TITLE = "browser_title";
    private static final String KEY_LOGIN_HINT = "login_hint";

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Override
    public Map<String, String> getAll() {
        Map<String, String> map = new HashMap<>();
        systemConfigMapper.selectList(null).forEach(item -> map.put(item.getConfigKey(), item.getConfigValue()));
        return map;
    }

    @Override
    public SystemConfigDTO getSystemInfo() {
        Map<String, String> config = getAll();
        SystemConfigDTO dto = new SystemConfigDTO();
        dto.setSystemName(config.getOrDefault(KEY_SYSTEM_NAME, "Wokit"));
        dto.setSystemSubName(config.getOrDefault(KEY_SYSTEM_SUB_NAME, "工作助手"));
        dto.setBrowserTitle(config.getOrDefault(KEY_BROWSER_TITLE, "Wokit 工作助手"));
        dto.setLoginHint(config.getOrDefault(KEY_LOGIN_HINT, "使用平台账号进入工作台"));
        return dto;
    }

    @Override
    @Transactional
    public void updateSystemInfo(SystemConfigDTO dto) {
        if (ObjectUtils.isEmpty(dto.getSystemName())) {
            throw new ServiceException("系统名称不能为空");
        }
        if (ObjectUtils.isEmpty(dto.getBrowserTitle())) {
            throw new ServiceException("浏览器标题后缀不能为空");
        }
        if (dto.getSystemName().length() > 30 || (dto.getSystemSubName() != null && dto.getSystemSubName().length() > 30)) {
            throw new ServiceException("系统名称和副标题不能超过 30 个字符");
        }
        if (dto.getBrowserTitle().length() > 80 || (dto.getLoginHint() != null && dto.getLoginHint().length() > 80)) {
            throw new ServiceException("标题后缀和登录说明不能超过 80 个字符");
        }
        upsert(KEY_SYSTEM_NAME, dto.getSystemName().trim(), "系统名称，显示在工作台与系统设置左上角");
        upsert(KEY_SYSTEM_SUB_NAME, dto.getSystemSubName() == null ? "" : dto.getSystemSubName().trim(), "系统副标题，显示在系统名称下方");
        upsert(KEY_BROWSER_TITLE, dto.getBrowserTitle().trim(), "浏览器标题后缀");
        upsert(KEY_LOGIN_HINT, dto.getLoginHint() == null ? "" : dto.getLoginHint().trim(), "登录页账号表单说明");
    }

    private void upsert(String key, String value, String description) {
        SystemConfig config = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
        if (config == null) {
            config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(description);
            systemConfigMapper.insert(config);
        } else {
            config.setConfigValue(value);
            config.setDescription(description);
            systemConfigMapper.updateById(config);
        }
    }
}
