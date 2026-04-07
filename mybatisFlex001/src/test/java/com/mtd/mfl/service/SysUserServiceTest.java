package com.mtd.mfl.service;

import com.mtd.mfl.entity.SysUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SysUserServiceTest {

    @Autowired
    SysUserService sysUserService;

    @Test
    public void t1() {
        SysUser sysUser = sysUserService.getById(1L);
        sysUser.setPassword("1");
        sysUserService.updateById(sysUser);
    }

}