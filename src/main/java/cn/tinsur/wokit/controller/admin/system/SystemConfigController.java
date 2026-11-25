package cn.tinsur.wokit.controller.admin.system;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.pojo.dto.SystemConfigDTO;
import cn.tinsur.wokit.service.ISystemConfigService;
import cn.tinsur.wokit.util.AuthUtil;
import cn.tinsur.wokit.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统信息配置接口
 * /public 允许登录页读取，其余仅管理员可用
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/admin/system")
public class SystemConfigController {

    @Autowired
    private ISystemConfigService systemConfigService;

    /**
     * 公开读取系统名称等信息
     */
    @GetMapping("/public")
    public Result<SystemConfigDTO> publicInfo() {
        return Result.ok(systemConfigService.getSystemInfo());
    }

    /**
     * 管理员读取系统信息
     */
    @GetMapping("/config")
    public Result<SystemConfigDTO> getConfig(HttpServletRequest request) {
        checkAdmin(request);
        return Result.ok(systemConfigService.getSystemInfo());
    }

    /**
     * 管理员保存系统信息
     */
    @PutMapping("/config")
    public Result updateConfig(@RequestBody SystemConfigDTO dto, HttpServletRequest request) {
        checkAdmin(request);
        systemConfigService.updateSystemInfo(dto);
        return Result.ok("系统信息已保存");
    }

    private void checkAdmin(HttpServletRequest request) {
        if (AuthUtil.getRole(request) != 1) {
            throw new ServiceException("只有管理员可以修改系统信息");
        }
    }
}
