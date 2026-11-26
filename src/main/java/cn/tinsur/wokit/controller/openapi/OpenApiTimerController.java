package cn.tinsur.wokit.controller.openapi;

import cn.tinsur.wokit.pojo.entity.TimerItem;
import cn.tinsur.wokit.security.RequirePermission;
import cn.tinsur.wokit.service.ITimerItemService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 开放 API：计时
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/api/v1/timers")
public class OpenApiTimerController {

    @Autowired
    private ITimerItemService timerItemService;

    @GetMapping
    public Result<List<TimerItem>> list(HttpServletRequest request) {
        return Result.ok(timerItemService.list(ApiAuthUtil.getUserId(request)));
    }

    @PostMapping
    @RequirePermission("timer:add")
    public Result add(@RequestBody TimerItem timerItem, HttpServletRequest request) {
        return timerItemService.add(timerItem, ApiAuthUtil.getUserId(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("timer:edit")
    public Result update(@PathVariable Long id, @RequestBody TimerItem timerItem, HttpServletRequest request) {
        return timerItemService.update(id, timerItem, ApiAuthUtil.getUserId(request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("timer:delete")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        return timerItemService.delete(id, ApiAuthUtil.getUserId(request));
    }
}
