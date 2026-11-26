package cn.tinsur.wokit.config;

import cn.tinsur.wokit.interceptor.ApiAuthInterceptor;
import cn.tinsur.wokit.interceptor.ApiPermissionInterceptor;
import cn.tinsur.wokit.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private ApiAuthInterceptor apiAuthInterceptor;

    @Autowired
    private ApiPermissionInterceptor apiPermissionInterceptor;

    @Override
    // 拦截所有请求，登录接口放行；开放 API 使用独立拦截器
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/admin/users/login", "/admin/system/public", "/api/v1/**");

        registry.addInterceptor(apiAuthInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout",
                        "/api/v1/oauth/token")
                .order(1);

        registry.addInterceptor(apiPermissionInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout",
                        "/api/v1/oauth/token")
                .order(2);
    }

    @Override
    // 把前台 String 改成 Date 类型
    public void addFormatters(FormatterRegistry registry) {
        List<String> patterns = Arrays.asList(
                "yyyy-MM-dd HH:mm:ss",        // 普通格式
                "yyyy-MM-dd",                  // 普通格式
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",  // ISO 格式
                "yyyy-MM-dd'T'HH:mm:ssX"       // ISO 格式
        );

        registry.addConverter(new Converter<String, Date>() {
            @Override
            public Date convert(String source) {
                for (String pattern : patterns) {
                    try {
                        return new SimpleDateFormat(pattern).parse(source);
                    } catch (ParseException ignored) {
                    }
                }
                throw new RuntimeException("时间转换失败：" + source);
            }
        });
    }
}
