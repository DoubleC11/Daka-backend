package com.daka.common;

import com.daka.service.CoupleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 情侣空间视图切换。
 * <p>
 * GET 请求带 {@code viewUserId} 时，校验双方存在生效绑定后，把「正在查看的用户」写入
 * {@link UserContext}，查询类接口即可返回对方数据。
 * 写操作（POST/PUT/DELETE）不参与，永远作用于登录用户本人。
 */
@Component
public class CoupleViewInterceptor implements HandlerInterceptor {

    public static final String PARAM = "viewUserId";

    private final CoupleService coupleService;

    public CoupleViewInterceptor(CoupleService coupleService) {
        this.coupleService = coupleService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return true;
        }
        Long me = UserContext.current();
        if (me == null) {
            return true;
        }
        String raw = request.getParameter(PARAM);
        if (raw == null || raw.isBlank()) {
            return true;
        }
        Long target;
        try {
            target = Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return true;
        }
        if (target.equals(me)) {
            return true;
        }
        if (!coupleService.isBound(me, target)) {
            throw new BizException(403, "无权查看该用户的数据");
        }
        UserContext.setViewId(target);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clearView();
    }
}
