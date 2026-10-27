package com.daka.common;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 静态资源缓存策略：
 * - /assets/**（Vite 产物，文件名带哈希）：长缓存 immutable
 * - 其他非 API 路径（index.html / SPA 回退）：no-cache，每次回源校验，
 *   避免发版后浏览器拿着旧 index.html 去加载已不存在的旧资源。
 */
@Component
public class CacheHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI();
        res.setHeader("X-App-Build", "20260928-1932");
        if (path.startsWith("/assets/")) {
            res.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        } else if (!path.startsWith("/api/")) {
            res.setHeader("Cache-Control", "no-cache");
        }
        chain.doFilter(request, response);
    }
}
