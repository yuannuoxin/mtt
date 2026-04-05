package com.mtd.demo.mypay;

import cn.hutool.core.util.IdUtil;
import org.springframework.boot.test.context.SpringBootTest;

/**
 *
 */
@SpringBootTest
public class MyPayTest1 {
    public static void main(String[] args) {
        System.out.println(IdUtil.fastSimpleUUID());
    }
}
