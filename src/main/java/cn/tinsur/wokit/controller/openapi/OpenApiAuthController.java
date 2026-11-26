package cn.tinsur.wokit.controller.openapi;

import cn.tinsur.wokit.pojo.dto.ApiLoginDTO;
import cn.tinsur.wokit.pojo.dto.ApiRefreshDTO;
import cn.tinsur.wokit.pojo.dto.UserPasswordDTO;
import cn.tinsur.wokit.pojo.vo.ApiTokenVO;
import cn.tinsur.wokit.service.IApiAuthService;
import cn.tinsur.wokit.util.ApiAuthUtil;
import cn.tinsur.wokit.util.RequestUtil;
import cn.tinsur.wokit.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * App 用户登录与开放 API 认证。
 * 不提供注册接口，账号只能由管理员在网页端“用户管理”中创建。
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/api/v1")
public class OpenApiAuthController {

    @Autowired
    private IApiAuthService apiAuthService;

    @PostMapping("/auth/login")
    public Result<ApiTokenVO> login(@RequestHeader(value = "X-Wokit-Client-Key", required = false) String accessKey,
                                    @RequestBody ApiLoginDTO dto,
                                    HttpServletRequest request) {
        return apiAuthService.login(accessKey, dto, RequestUtil.clientIp(request));
    }

    @PostMapping("/auth/refresh")
    public Result<ApiTokenVO> refresh(@RequestBody ApiRefreshDTO dto, HttpServletRequest request) {
        return apiAuthService.refresh(dto, RequestUtil.clientIp(request));
    }

    @PostMapping("/auth/logout")
    public Result logout(@RequestBody(required = false) ApiRefreshDTO dto) {
        return apiAuthService.logout(dto);
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me(HttpServletRequest request) {
        return Result.ok(apiAuthService.me(ApiAuthUtil.getUserId(request)));
    }

    @PutMapping("/me/password")
    public Result updatePassword(@RequestBody UserPasswordDTO dto, HttpServletRequest request) {
        ApiAuthUtil.requireUserToken(request);
        return apiAuthService.updatePassword(ApiAuthUtil.getUserId(request), dto);
    }

    /**
     * 服务端 Client Credentials。Secret 只应保存在服务端环境变量中。
     */
    @PostMapping("/oauth/token")
    public Result<Map<String, Object>> serviceToken(@RequestParam String grant_type,
                                                    @RequestParam String client_id,
                                                    @RequestParam String client_secret,
                                                    HttpServletRequest request) {
        if (!"client_credentials".equals(grant_type)) {
            throw new cn.tinsur.wokit.exception.ApiException(400, "只支持 client_credentials 模式");
        }
        return Result.ok("获取成功",
                apiAuthService.serviceToken(client_id, client_secret, RequestUtil.clientIp(request)));
    }
}
