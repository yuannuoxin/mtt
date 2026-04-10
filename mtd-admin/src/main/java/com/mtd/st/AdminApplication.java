package com.mtd.st;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan(basePackages = {
        "com.mtd.st",
        "com.mtd.common.mybatis",
        "com.mtd.common.satoken",
        "com.mtd.common.web",
        "com.mtd.common.redis"
})
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
