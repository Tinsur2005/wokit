package cn.tinsur.wokit.controller.admin.tool;

import cn.tinsur.wokit.pojo.entity.WorkLog;
import cn.tinsur.wokit.pojo.query.WorkLogQuery;
import cn.tinsur.wokit.pojo.vo.WorkLogStatVO;
import cn.tinsur.wokit.service.IWorkLogService;
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
 * <p>
 * 工时登记 前端控制器（扩展小工具）
 * </p>
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/admin/worklogs")
public class WorkLogController {

    @Autowired
    private IWorkLogService workLogService;

    /**
     * 分页查询工时记录
     * GET /admin/worklogs?page=1&limit=10&month=2026-11&keyword=xxx
     */
    @GetMapping
    public Result<IPage<WorkLog>> list(WorkLogQuery workLogQuery, HttpServletRequest request) {
        return Result.ok(workLogService.list(workLogQuery, AuthUtil.getUserId(request)));
    }

    /**
     * 查询某月工时统计
     * GET /admin/worklogs/stat?month=2026-11
     */
    @GetMapping("/stat")
    public Result<WorkLogStatVO> stat(@RequestParam(required = false) String month, HttpServletRequest request) {
        return Result.ok(workLogService.stat(AuthUtil.getUserId(request), month));
    }

    /**
     * 新增工时记录
     * POST /admin/worklogs
     */
    @PostMapping
    public Result add(@RequestBody WorkLog workLog, HttpServletRequest request) {
        return workLogService.add(workLog, AuthUtil.getUserId(request));
    }

    /**
     * 修改工时记录
     * PUT /admin/worklogs/1
     */
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody WorkLog workLog, HttpServletRequest request) {
        return workLogService.update(id, workLog, AuthUtil.getUserId(request));
    }

    /**
     * 删除工时记录（真实删除）
     * DELETE /admin/worklogs/1
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        return workLogService.delete(id, AuthUtil.getUserId(request));
    }
}
