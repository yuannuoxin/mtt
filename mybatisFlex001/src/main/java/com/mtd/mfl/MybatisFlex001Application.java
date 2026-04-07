package com.mtd.mfl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan(basePackages = {
        "com.mtd.mfl",
        "com.mtd.common.web"
})
public class MybatisFlex001Application {

    public static void main(String[] args) {
        SpringApplication.run(MybatisFlex001Application.class, args);

    }
}
