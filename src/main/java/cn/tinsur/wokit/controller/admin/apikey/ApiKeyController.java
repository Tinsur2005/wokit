package cn.tinsur.wokit.controller.admin.apikey;

import cn.tinsur.wokit.pojo.dto.ApiKeyCreateDTO;
import cn.tinsur.wokit.pojo.vo.ApiKeyVO;
import cn.tinsur.wokit.security.PermissionGuard;
import cn.tinsur.wokit.service.IApiKeyService;
import cn.tinsur.wokit.util.AuthUtil;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Key 管理（网页端）
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/admin/api-keys")
public class ApiKeyController {

    @Autowired
    private IApiKeyService apiKeyService;

    @Autowired
    private PermissionGuard permissionGuard;

    @GetMapping
    public Result<IPage<ApiKeyVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer limit,
                                        @RequestParam(required = false) String appName,
                                        @RequestParam(required = false) String keyType,
                                        @RequestParam(required = false) Integer status,
                                        HttpServletRequest request) {
        Long userId = AuthUtil.getUserId(request);
        permissionGuard.requireMenu(userId, PermissionGuard.API_KEY_MENU);
        return Result.ok(apiKeyService.list(page, limit, appName, keyType, status, userId));
    }

    @PostMapping
    public Result<ApiKeyVO> create(@RequestBody ApiKeyCreateDTO dto, HttpServletRequest request) {
        Long userId = AuthUtil.getUserId(request);
        permissionGuard.requireMenu(userId, PermissionGuard.API_KEY_MENU);
        permissionGuard.requireValue(userId, "api:key:add");
        return Result.ok("创建成功，Secret Key 只显示这一次", apiKeyService.create(dto, userId));
    }

    @PutMapping("/{id}/status")
    public Result changeStatus(@PathVariable Long id,
                               @RequestParam Integer status,
                               HttpServletRequest request) {
        Long userId = AuthUtil.getUserId(request);
        permissionGuard.requireMenu(userId, PermissionGuard.API_KEY_MENU);
        permissionGuard.requireValue(userId, "api:key:toggle");
        apiKeyService.changeStatus(id, status, userId);
        return Result.ok(status == 1 ? "已启用该密钥" : "已停用该密钥");
    }

    @PostMapping("/{id}/reset")
    public Result<ApiKeyVO> resetSecret(@PathVariable Long id, HttpServletRequest request) {
        Long userId = AuthUtil.getUserId(request);
        permissionGuard.requireMenu(userId, PermissionGuard.API_KEY_MENU);
        permissionGuard.requireValue(userId, "api:key:reset");
        return Result.ok("Secret Key 已重置，旧 Secret 立即失效", apiKeyService.resetSecret(id, userId));
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = AuthUtil.getUserId(request);
        permissionGuard.requireMenu(userId, PermissionGuard.API_KEY_MENU);
        permissionGuard.requireValue(userId, "api:key:delete");
        apiKeyService.delete(id, userId);
        return Result.ok("密钥已删除");
    }
}
