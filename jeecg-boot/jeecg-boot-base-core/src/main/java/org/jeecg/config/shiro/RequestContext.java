package org.jeecg.config.shiro;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;

public class RequestContext {

    private static ThreadLocal<RequestHeader> threadLocal = new InheritableThreadLocal<>();

    public static void set(RequestHeader requestHeader) {
        threadLocal.set(requestHeader);
    }

    public static RequestHeader get() {
        return threadLocal.get();
    }

    public static String getRequestId() {
        RequestHeader requestHeader = get();
        if (requestHeader == null) {
            return null;
        }
        return requestHeader.getRequestId();
    }

    public static LoginUser getLoginUser() {
        RequestHeader requestHeader = get();
        if (requestHeader == null) {
            return null;
        }
        return requestHeader.getLoginUser();
    }

    public static LoginUser getSupplierLoginUser() {

        RequestHeader requestHeader = get();

        if (requestHeader == null) {
            throw new JeecgBootException(401,"您还没有登录");
        }

        LoginUser loginUser = requestHeader.getLoginUser();

        if (loginUser == null) {
            throw new JeecgBootException(401,"您还没有登录");
        }

        if (loginUser.getUserType() == null || loginUser.getUserType() != 2) {
            throw new JeecgBootException(400,"您不是供应商用户");
        }

        return loginUser;
    }

    public static void clear() {
        threadLocal.remove();
    }

}
