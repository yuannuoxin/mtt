package com.mtd.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Demo 应用启动类
 * 
 * 扫描范围说明：
 * - com.mtd.demo: demo 模块自身的包
 * - com.mtd.common.mybatis: MyBatis-Plus 配置和自动填充处理器
 * - com.mtd.common.exception: 全局异常处理器
 * - com.mtd.common.satoken: Sa-Token 认证相关组件
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.mtd.demo", "com.mtd.common.mybatis", "com.mtd.common.exception.handler", "com.mtd.common.satoken"})
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
