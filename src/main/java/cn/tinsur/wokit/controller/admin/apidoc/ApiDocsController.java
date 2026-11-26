package cn.tinsur.wokit.controller.admin.apidoc;

import cn.tinsur.wokit.security.PermissionGuard;
import cn.tinsur.wokit.util.AuthUtil;
import cn.tinsur.wokit.util.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 开放 API 文档。文档内容本身也受 RBAC 菜单权限保护。
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/admin/api-docs")
public class ApiDocsController {

    @Autowired
    private PermissionGuard permissionGuard;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/spec")
    public Result<Map<String, Object>> spec(HttpServletRequest request) {
        Long userId = AuthUtil.getUserId(request);
        permissionGuard.requireMenu(userId, PermissionGuard.API_DOC_MENU);
        return Result.ok(loadDocs());
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(HttpServletRequest request) throws Exception {
        Long userId = AuthUtil.getUserId(request);
        permissionGuard.requireMenu(userId, PermissionGuard.API_DOC_MENU);
        permissionGuard.requireValue(userId, "api:doc:export");
        byte[] body = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(loadDocs());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("wokit-api-docs.json", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private Map<String, Object> loadDocs() {
        try (InputStream inputStream = new ClassPathResource("api-docs.json").getInputStream()) {
            return objectMapper.readValue(inputStream, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("接口文档加载失败", e);
        }
    }
}
