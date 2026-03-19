package com.mtd.demo.config;

import com.mtd.demo.jackson.LongToStringModule;
import com.mtd.demo.jackson.MttJavaTimeModule;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Jackson 时间配置
 * 用于统一配置 Long、LocalDateTime、LocalDate、LocalTime 的响应格式
 */
@AutoConfigureBefore(JacksonAutoConfiguration.class)
@Configuration
public class JacksonTimeConfig {

    @Bean
    public JsonMapperBuilderCustomizer customizer() {
        return builder -> builder.defaultLocale(Locale.CHINA)
                .defaultTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()))
                .addModule(new MttJavaTimeModule())
                .addModule(new LongToStringModule());
    }
}
