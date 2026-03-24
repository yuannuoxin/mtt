package com.mtd.iam;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * IAM 身份识别与访问管理模块启动类
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.mtd.iam",
        "com.mtd.common.mybatis",
        "com.mtd.common.satoken",
        "com.mtd.common.web",
        "com.mtd.common.redis"
})
public class IamApplication {

    public static void main(String[] args) {
        SpringApplication.run(IamApplication.class, args);
    }
}
