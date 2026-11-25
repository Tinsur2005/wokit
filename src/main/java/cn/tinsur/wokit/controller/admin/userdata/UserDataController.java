package cn.tinsur.wokit.controller.admin.userdata;

import cn.tinsur.wokit.service.IUserDataService;
import cn.tinsur.wokit.util.AuthUtil;
import cn.tinsur.wokit.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用户工作台数据导出与恢复
 */
@RestController
@RequestMapping("/admin/user-data")
public class UserDataController {

    private static final DateTimeFormatter FILE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Autowired
    private IUserDataService userDataService;

    /**
     * 导出当前用户全部工作台数据
     * GET /admin/user-data/export
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(HttpServletRequest request) {
        byte[] content = userDataService.export(AuthUtil.getUserId(request));
        String filename = "wokit-backup-" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".wokit";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    /**
     * 恢复当前用户工作台数据
     * POST /admin/user-data/restore
     */
    @PostMapping(value = "/restore", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Result restore(@RequestBody byte[] content, HttpServletRequest request) {
        return userDataService.restore(content, AuthUtil.getUserId(request));
    }
}
