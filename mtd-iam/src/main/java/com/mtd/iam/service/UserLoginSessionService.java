package com.mtd.iam.service;

import cn.hutool.core.util.StrUtil;
import com.mtd.iam.constant.StrConstant;
import com.mtd.iam.entity.LoginSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户登录会话服务（用于处理二次验证）
 */
@Slf4j
@Service
public class UserLoginSessionService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String SESSION_PREFIX = StrConstant.PROJECT_NAME + ":tmpLogin:session:";
    private static final long SESSION_TTL_MINUTES = 10; // 会话有效期 10 分钟

    /**
     * 创建临时登录会话（仅存储 userId，用于前端选择验证方式）
     */
    public String createTempSession(Long userId) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String redisKey = SESSION_PREFIX + sessionId;

        // 仅存储 userId
        stringRedisTemplate.opsForValue().set(redisKey, userId.toString(), SESSION_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("创建临时登录会话，sessionId: {}, userId: {}", sessionId, userId);

        return sessionId;
    }

    /**
     * 获取登录会话（支持临时会话和正式会话）
     */
    public LoginSession getSession(String sessionId) {
        // 先尝试从正式会话中获取
        String redisKey = SESSION_PREFIX + sessionId;
        String value = stringRedisTemplate.opsForValue().get(redisKey);

        if (StrUtil.isEmpty(value)) {
            return null;
        }

        LoginSession session = new LoginSession();
        session.setSessionId(sessionId);
        session.setUserId(Long.parseLong(value));

        return session;
    }

    /**
     * 删除登录会话
     */
    public void deleteSession(String sessionId) {
        String redisKey = SESSION_PREFIX + sessionId;
        stringRedisTemplate.delete(redisKey);
        log.info("删除登录会话，sessionId: {}", sessionId);
    }
}
