package com.daka.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import org.springframework.http.CacheControl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 注册登录拦截器 + 放开跨域（前端 Vite 开发服务器）+ SPA 路由回退。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final CoupleViewInterceptor coupleViewInterceptor;

    public WebConfig(AuthInterceptor authInterceptor, CoupleViewInterceptor coupleViewInterceptor) {
        this.authInterceptor = authInterceptor;
        this.coupleViewInterceptor = coupleViewInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register"
                );
        // 情侣空间视图切换：必须排在登录拦截器之后，才能拿到当前登录态
        registry.addInterceptor(coupleViewInterceptor)
                .addPathPatterns("/api/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * SPA 回退 + 缓存策略：
     * - /assets/**（带哈希）长缓存 immutable
     * - 其余（含回退的 index.html）no-cache，浏览器每次回源校验，避免新旧版本错配
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).immutable());

        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // API 路径不回退，交给 Spring MVC（无匹配时 404）
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // 非静态文件路径（SPA 前端路由）回退 index.html
                        if (!resourcePath.contains(".")) {
                            Resource index = location.createRelative("index.html");
                            if (index.exists() && index.isReadable()) {
                                return index;
                            }
                        }
                        return null;
                    }
                });
    }
}
