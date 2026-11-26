package cn.tinsur.wokit.exception;

/**
 * 开放 API 异常：携带 HTTP 状态码，便于 App 与服务端对接时区分认证失败、权限不足等。
 *
 * @author Tinsur
 */
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
