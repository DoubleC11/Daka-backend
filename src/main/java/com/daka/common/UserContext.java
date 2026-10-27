package com.daka.common;

/**
 * 当前登录用户上下文（由 AuthInterceptor 写入，请求结束后清理）。
 * <p>
 * VIEW 是「情侣空间」的只读视图：查询接口用 {@link #requireView()}，
 * 增删改一律用 {@link #require()}，保证只能看、不能改对方数据。
 */
public final class UserContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Long> VIEW = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId) {
        CURRENT.set(userId);
    }

    /** 当前登录用户 id，未登录时为 null */
    public static Long current() {
        return CURRENT.get();
    }

    /** 取当前用户 id，未登录时抛 401 业务异常。写操作一律用它。 */
    public static Long require() {
        Long id = CURRENT.get();
        if (id == null) {
            throw new BizException(401, "未登录或登录已失效");
        }
        return id;
    }

    public static void setViewId(Long userId) {
        VIEW.set(userId);
    }

    /** 取「正在查看的用户 id」：查询接口用它，未切换视图时等同当前用户 */
    public static Long requireView() {
        Long v = VIEW.get();
        return v != null ? v : require();
    }

    public static void clearView() {
        VIEW.remove();
    }

    public static void clear() {
        CURRENT.remove();
        VIEW.remove();
    }
}
