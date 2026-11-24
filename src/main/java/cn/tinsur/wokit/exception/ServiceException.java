package cn.tinsur.wokit.exception;

/**
 * 业务异常：service 中主动抛出，msg 直接展示给用户
 *
 * @author Tinsur
 */
public class ServiceException extends RuntimeException {

    public ServiceException(String message) {
        super(message);
    }
}
