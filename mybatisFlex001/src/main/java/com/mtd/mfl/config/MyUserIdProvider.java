package com.mtd.mfl.config;

import org.springframework.stereotype.Component;

@Component
public class MyUserIdProvider {
    private Long i = 1L;

    public Long getUserId() {

        return i++;
    }
}
