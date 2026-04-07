package com.mtd.mjq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan(basePackages = {
        "com.mtd.mjq",
        "com.mtd.common.web"
})
public class MyJpaQueryDSL001Application {

    public static void main(String[] args) {
        SpringApplication.run(MyJpaQueryDSL001Application.class, args);

    }
}
