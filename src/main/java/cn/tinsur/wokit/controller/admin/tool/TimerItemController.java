package cn.tinsur.wokit.controller.admin.tool;

import cn.tinsur.wokit.pojo.entity.TimerItem;
import cn.tinsur.wokit.service.ITimerItemService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 计时 前端控制器（扩展小工具）
 * </p>
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/admin/timers")
public class TimerItemController {

    @Autowired
    private ITimerItemService timerItemService;

    /**
     * 查询当前用户的计时列表
     * GET /admin/timers
     */
    @GetMapping
    public Result<List<TimerItem>> list(HttpServletRequest request) {
        return Result.ok(timerItemService.list(AuthUtil.getUserId(request)));
    }

    /**
     * 新增计时
     * POST /admin/timers
     */
    @PostMapping
    public Result add(@RequestBody TimerItem timerItem, HttpServletRequest request) {
        return timerItemService.add(timerItem, AuthUtil.getUserId(request));
    }

    /**
     * 修改计时
     * PUT /admin/timers/1
     */
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody TimerItem timerItem, HttpServletRequest request) {
        return timerItemService.update(id, timerItem, AuthUtil.getUserId(request));
    }

    /**
     * 删除计时（真实删除）
     * DELETE /admin/timers/1
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        return timerItemService.delete(id, AuthUtil.getUserId(request));
    }
}
