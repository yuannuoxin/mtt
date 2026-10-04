package com.mtd.tt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
        "com.mtd.tt",
        "com.mtd.common.mybatis",
        "com.mtd.common.satoken",
        "com.mtd.common.web",
        "com.mtd.common.redis"
})
public class TtApplication {

    public static void main(String[] args) {
        SpringApplication.run(TtApplication.class, args);
    }
}
