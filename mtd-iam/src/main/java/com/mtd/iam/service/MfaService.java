package com.mtd.iam.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mtd.iam.constant.StrConstant;
import com.mtd.iam.entity.UserMfa;
import com.mtd.iam.enums.YesEnum;
import com.mtd.iam.mapper.UserMfaMapper;
import com.mtd.iam.util.TotpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MFA 服务类
 */
@Slf4j
@Service
public class MfaService {

    @Autowired
    private UserMfaMapper userMfaMapper;

    /**
     * 检查当前用户是否已启用 MFA
     */
    public boolean isUserMfaEnabled() {
        Long userId = StpUtil.getLoginIdAsLong();
        return isUserMfaEnabled(userId);
    }

    /**
     * 检查指定用户是否已启用 MFA
     */
    public boolean isUserMfaEnabled(Long userId) {
        UserMfa userMfa = userMfaMapper.selectOne(
                Wrappers.<UserMfa>lambdaQuery()
                        .eq(UserMfa::getUserId, userId)
                        .eq(UserMfa::getStatus, YesEnum.YES.getCode())
        );
        return ObjUtil.isNotNull(userMfa);
    }

    /**
     * 添加 MFA（生成密钥）
     */
    @Transactional(rollbackFor = Exception.class)
    public UserMfa addMfa(Long userId) {
        // 检查是否已存在 MFA
        UserMfa existingMfa = userMfaMapper.selectOne(
                Wrappers.<UserMfa>lambdaQuery()
                        .eq(UserMfa::getUserId, userId)
                        .last(StrConstant.LIMIT_1)
        );

        if (ObjUtil.isNotNull(existingMfa)) {
            log.warn("用户已存在 MFA 配置，userId: {}", userId);
            return existingMfa;
        }

        // 生成新的 MFA 配置
        UserMfa userMfa = new UserMfa();
        userMfa.setUserId(userId);
        userMfa.setSecret(TotpUtil.generateSecret());
        userMfa.setStatus(YesEnum.NO.getCode()); // 默认禁用状态
        userMfaMapper.insert(userMfa);

        log.info("为用户添加 MFA 配置，userId: {}, mfaId: {}", userId, userMfa.getId());
        return userMfa;
    }

    /**
     * 更新 MFA 状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMfaStatus(Long mfaId, Integer status) {
        UserMfa userMfa = userMfaMapper.selectById(mfaId);
        if (ObjUtil.isNull(userMfa)) {
            throw new RuntimeException("MFA 配置不存在");
        }

        userMfa.setStatus(status);
        userMfaMapper.updateById(userMfa);

        log.info("更新 MFA 状态，mfaId: {}, status: {}", mfaId, status);
    }

    /**
     * 验证 MFA 验证码
     */
    public boolean verifyMfaCode(Long mfaId, String code) {
        UserMfa userMfa = userMfaMapper.selectById(mfaId);
        if (ObjUtil.isNull(userMfa) || StrUtil.isEmpty(userMfa.getSecret())) {
            log.warn("MFA 配置不存在或密钥为空，mfaId: {}", mfaId);
            return false;
        }

        return TotpUtil.verifyCode(userMfa.getSecret(), code);
    }

    /**
     * 获取用户的 MFA 配置
     */
    public UserMfa getUserMfa(Long userId) {
        return userMfaMapper.selectOne(
                Wrappers.<UserMfa>lambdaQuery()
                        .eq(UserMfa::getUserId, userId)
                        .orderByDesc(UserMfa::getId)
                        .last(StrConstant.LIMIT_1)
        );
    }
}
