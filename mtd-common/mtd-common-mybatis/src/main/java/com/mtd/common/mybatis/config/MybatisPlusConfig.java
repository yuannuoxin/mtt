package com.mtd.common.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 */
@Configuration
public class MybatisPlusConfig {

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    /**
     * 分页插件配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 根据驱动类型动态选择数据库类型
        DbType dbType = getDbType(driverClassName);
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));
        return interceptor;
    }

    /**
     * 根据驱动类名判断数据库类型
     */
    private DbType getDbType(String driverClassName) {
        if (driverClassName.contains("postgresql")) {
            return DbType.POSTGRE_SQL;
        } else if (driverClassName.contains("mysql")) {
            return DbType.MYSQL;
        } else if (driverClassName.contains("oracle")) {
            return DbType.ORACLE;
        } else if (driverClassName.contains("sqlserver")) {
            return DbType.SQL_SERVER;
        }
        // 默认返回 MYSQL
        return DbType.MYSQL;
    }
}
