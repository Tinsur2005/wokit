package cn.tinsur.wokit.controller.openapi;

import cn.tinsur.wokit.pojo.entity.WorkLog;
import cn.tinsur.wokit.pojo.query.WorkLogQuery;
import cn.tinsur.wokit.pojo.vo.WorkLogStatVO;
import cn.tinsur.wokit.security.RequirePermission;
import cn.tinsur.wokit.service.IWorkLogService;
import cn.tinsur.wokit.util.ApiAuthUtil;
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
 * 开放 API：工时
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/api/v1/worklogs")
public class OpenApiWorkLogController {

    @Autowired
    private IWorkLogService workLogService;

    @GetMapping
    public Result<IPage<WorkLog>> list(WorkLogQuery workLogQuery, HttpServletRequest request) {
        return Result.ok(workLogService.list(workLogQuery, ApiAuthUtil.getUserId(request)));
    }

    @GetMapping("/stat")
    public Result<WorkLogStatVO> stat(@RequestParam(required = false) String month, HttpServletRequest request) {
        return Result.ok(workLogService.stat(ApiAuthUtil.getUserId(request), month));
    }

    @PostMapping
    @RequirePermission("worklog:add")
    public Result add(@RequestBody WorkLog workLog, HttpServletRequest request) {
        return workLogService.add(workLog, ApiAuthUtil.getUserId(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("worklog:edit")
    public Result update(@PathVariable Long id, @RequestBody WorkLog workLog, HttpServletRequest request) {
        return workLogService.update(id, workLog, ApiAuthUtil.getUserId(request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("worklog:delete")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        return workLogService.delete(id, ApiAuthUtil.getUserId(request));
    }
}
