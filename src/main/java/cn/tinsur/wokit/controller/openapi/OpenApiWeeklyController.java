package cn.tinsur.wokit.controller.openapi;

import cn.tinsur.wokit.pojo.entity.Weekly;
import cn.tinsur.wokit.security.RequirePermission;
import cn.tinsur.wokit.service.IWeeklyService;
import cn.tinsur.wokit.util.ApiAuthUtil;
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
 * 开放 API：周报
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/api/v1/weeklies")
public class OpenApiWeeklyController {

    @Autowired
    private IWeeklyService weeklyService;

    @GetMapping
    public Result<List<Weekly>> list(@RequestParam(required = false) String weekStart, HttpServletRequest request) {
        return Result.ok(weeklyService.list(weekStart, ApiAuthUtil.getUserId(request)));
    }

    @PostMapping
    @RequirePermission("weekly:add")
    public Result add(@RequestBody Weekly weekly, HttpServletRequest request) {
        return weeklyService.add(weekly, ApiAuthUtil.getUserId(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("weekly:edit")
    public Result update(@PathVariable Long id, @RequestBody Weekly weekly, HttpServletRequest request) {
        return weeklyService.update(id, weekly, ApiAuthUtil.getUserId(request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("weekly:delete")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        return weeklyService.delete(id, ApiAuthUtil.getUserId(request));
    }
}
