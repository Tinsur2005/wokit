package cn.tinsur.wokit.controller.admin.tool;

import cn.tinsur.wokit.pojo.entity.Weekly;
import cn.tinsur.wokit.service.IWeeklyService;
import cn.tinsur.wokit.util.AuthUtil;
import cn.tinsur.wokit.util.Result;
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

import java.util.List;

/**
 * <p>
 * 周报记录 前端控制器（扩展小工具）
 * </p>
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/admin/weeklies")
public class WeeklyController {

    @Autowired
    private IWeeklyService weeklyService;

    /**
     * 查询某一周的周报明细
     * GET /admin/weeklies?weekStart=2026-11-23
     */
    @GetMapping
    public Result<List<Weekly>> list(@RequestParam(required = false) String weekStart, HttpServletRequest request) {
        return Result.ok(weeklyService.list(weekStart, AuthUtil.getUserId(request)));
    }

    /**
     * 新增周报记录
     * POST /admin/weeklies
     */
    @PostMapping
    public Result add(@RequestBody Weekly weekly, HttpServletRequest request) {
        return weeklyService.add(weekly, AuthUtil.getUserId(request));
    }

    /**
     * 修改周报记录
     * PUT /admin/weeklies/1
     */
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody Weekly weekly, HttpServletRequest request) {
        return weeklyService.update(id, weekly, AuthUtil.getUserId(request));
    }

    /**
     * 删除周报记录（真实删除）
     * DELETE /admin/weeklies/1
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        return weeklyService.delete(id, AuthUtil.getUserId(request));
    }
}
