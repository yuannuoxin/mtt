package com.mtd.demo.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.ser.std.NumberSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Jackson 时间配置
 * 用于统一配置 Long、LocalDateTime、LocalDate、LocalTime 的响应格式
 */
@Configuration
public class JacksonTimeConfig {

    // 定义各自的格式常量，方便统一管理
    private static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String TIME_PATTERN = "HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            // ==========================================
            // 1. 全局基础配置
            // ==========================================
            
            // Long 转 String (防止前端精度丢失)
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(long.class, ToStringSerializer.instance);
            
            // BigInteger 转 String (防止超大整数精度丢失)
            builder.serializerByType(BigInteger.class, ToStringSerializer.instance);
            
            // BigDecimal 转 String (防止浮点数精度丢失)
            builder.serializerByType(BigDecimal.class, ToStringSerializer.instance);
            
            // 设置默认时区 (影响未单独配置的日期类型)
            builder.timeZone(TimeZone.getTimeZone("Asia/Shanghai"));

            // ==========================================
            // 2. 单独配置 LocalDateTime (日期 + 时间)
            // ==========================================
            DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern(DATETIME_PATTERN);
            
            // 序列化：响应给前端时的格式
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(dtFormatter));
            // 反序列化：接收前端参数时的格式
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(dtFormatter));

            // ==========================================
            // 3. 单独配置 LocalDate (仅日期)
            // ==========================================
            DateTimeFormatter dFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
            
            builder.serializerByType(LocalDate.class, new LocalDateSerializer(dFormatter));
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer(dFormatter));

            // ==========================================
            // 4. 单独配置 LocalTime (仅时间)
            // ==========================================
            DateTimeFormatter tFormatter = DateTimeFormatter.ofPattern(TIME_PATTERN);
            
            builder.serializerByType(LocalTime.class, new LocalTimeSerializer(tFormatter));
            builder.deserializerByType(LocalTime.class, new LocalTimeDeserializer(tFormatter));
            
            // ==========================================
            // 5. (可选) 单独配置 java.util.Date
            // ==========================================
            // 如果你项目中还有老的 Date 类型，可以单独指定，通常与 LocalDateTime 格式一致
            builder.simpleDateFormat(DATETIME_PATTERN); 
        };
    }
}
