package cn.tinsur.wokit.controller.openapi;

import cn.tinsur.wokit.pojo.vo.DashboardVO;
import cn.tinsur.wokit.service.IDashboardService;
import cn.tinsur.wokit.util.ApiAuthUtil;
import cn.tinsur.wokit.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开放 API：首页看板
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class OpenApiDashboardController {

    @Autowired
    private IDashboardService dashboardService;

    @GetMapping
    public Result<DashboardVO> get(HttpServletRequest request) {
        return Result.ok(dashboardService.get(ApiAuthUtil.getUserId(request)));
    }
}
