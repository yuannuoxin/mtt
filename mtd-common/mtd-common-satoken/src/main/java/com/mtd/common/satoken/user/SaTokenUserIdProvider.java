package com.mtd.common.satoken.user;

import cn.dev33.satoken.stp.StpUtil;
import com.mtd.common.mybatis.user.UserIdProvider;
import org.springframework.stereotype.Component;

/**
 * Sa-Token 用户 ID 提供者实现
 */
@Component
public class SaTokenUserIdProvider implements UserIdProvider {

    @Override
    public Long getUserId() {
        if (StpUtil.isLogin()) {
            Object loginId = StpUtil.getLoginId();
            return loginId != null ? Long.valueOf(loginId.toString()) : null;
        }
        return null;
    }
}
