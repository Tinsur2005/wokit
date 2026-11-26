package cn.tinsur.wokit.controller.openapi;

import cn.tinsur.wokit.pojo.entity.Memo;
import cn.tinsur.wokit.security.RequirePermission;
import cn.tinsur.wokit.service.IMemoService;
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
 * 开放 API：备忘录
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/api/v1/memos")
public class OpenApiMemoController {

    @Autowired
    private IMemoService memoService;

    @GetMapping
    public Result<List<Memo>> list(@RequestParam(required = false) String keyword, HttpServletRequest request) {
        return Result.ok(memoService.list(ApiAuthUtil.getUserId(request), keyword));
    }

    @PostMapping
    @RequirePermission("memo:add")
    public Result add(@RequestBody Memo memo, HttpServletRequest request) {
        return memoService.add(memo, ApiAuthUtil.getUserId(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("memo:edit")
    public Result update(@PathVariable Long id, @RequestBody Memo memo, HttpServletRequest request) {
        return memoService.update(id, memo, ApiAuthUtil.getUserId(request));
    }

    @PutMapping("/pin/{id}")
    @RequirePermission("memo:pin")
    public Result togglePin(@PathVariable Long id, HttpServletRequest request) {
        return memoService.togglePin(id, ApiAuthUtil.getUserId(request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("memo:delete")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        return memoService.delete(id, ApiAuthUtil.getUserId(request));
    }
}
