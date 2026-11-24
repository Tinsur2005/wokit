package cn.tinsur.wokit.controller.admin.tool;

import cn.tinsur.wokit.pojo.entity.Memo;
import cn.tinsur.wokit.service.IMemoService;
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
 * 备忘录 前端控制器（扩展小工具）
 * </p>
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/admin/memos")
public class MemoController {

    @Autowired
    private IMemoService memoService;

    /**
     * 查询当前用户的备忘录
     * GET /admin/memos?keyword=xxx
     */
    @GetMapping
    public Result<List<Memo>> list(@RequestParam(required = false) String keyword, HttpServletRequest request) {
        return Result.ok(memoService.list(AuthUtil.getUserId(request), keyword));
    }

    /**
     * 新增备忘录
     * POST /admin/memos
     */
    @PostMapping
    public Result add(@RequestBody Memo memo, HttpServletRequest request) {
        return memoService.add(memo, AuthUtil.getUserId(request));
    }

    /**
     * 修改备忘录
     * PUT /admin/memos/1
     */
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody Memo memo, HttpServletRequest request) {
        return memoService.update(id, memo, AuthUtil.getUserId(request));
    }

    /**
     * 置顶 / 取消置顶
     * PUT /admin/memos/pin/1
     */
    @PutMapping("/pin/{id}")
    public Result togglePin(@PathVariable Long id, HttpServletRequest request) {
        return memoService.togglePin(id, AuthUtil.getUserId(request));
    }

    /**
     * 删除备忘录（真实删除）
     * DELETE /admin/memos/1
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        return memoService.delete(id, AuthUtil.getUserId(request));
    }
}
