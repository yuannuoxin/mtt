package com.mtd.st.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match("/**")
                    .notMatch("/auth/passwordLogin", "/auth/emailCodeLogin", "/auth/sendEmailCode")
                    .notMatch("/doc.html", "/swagger-ui.html", "/v3/api-docs/**", "/webjars/**")
                    .check(r -> {});
        })).addPathPatterns("/**");
    }
}
