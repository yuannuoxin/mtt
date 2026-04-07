package com.mtd.mfl.service;

import cn.hutool.core.util.IdUtil;
import com.mtd.mfl.entity.SysUser;
import com.mtd.mfl.entity.table.SysUserTableDef;
import com.mybatisflex.core.update.UpdateChain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

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


//    @Transactional(rollbackFor = Throwable.class)
    @Test
    public void t2() {
        UpdateChain.of(SysUser.class)
                .set(SysUser::getNickname, "张三")
                .setRaw(SysUser::getStatus, SysUserTableDef.SYS_USER.STATUS.add(1))
                .where(SysUser::getId).eq(1)
                .update();
    }

    @Test
    public void t3() {
        SysUser sysUser = new SysUser();
        sysUser.setUsername(IdUtil.getSnowflakeNextIdStr());
        sysUser.setPassword("1");
        sysUserService.save(sysUser);
    }

}