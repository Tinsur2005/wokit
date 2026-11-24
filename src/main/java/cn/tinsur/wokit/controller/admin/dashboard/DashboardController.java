package cn.tinsur.wokit.controller.admin.dashboard;

import cn.tinsur.wokit.pojo.vo.DashboardVO;
import cn.tinsur.wokit.service.IDashboardService;
import cn.tinsur.wokit.util.AuthUtil;
import cn.tinsur.wokit.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 首页数据看板 前端控制器
 * </p>
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/admin/dashboard")
public class DashboardController {

    @Autowired
    private IDashboardService dashboardService;

    /**
     * 查询当前用户的首页数据看板
     * GET /admin/dashboard
     */
    @GetMapping
    public Result<DashboardVO> get(HttpServletRequest request) {
        return Result.ok(dashboardService.get(AuthUtil.getUserId(request)));
    }
}
