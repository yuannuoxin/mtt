package com.mtd.iam.service;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mtd.iam.constant.StrConstant;
import com.mtd.iam.entity.User;
import com.mtd.iam.entity.UserCredential;
import com.mtd.iam.entity.UserMfa;
import com.mtd.iam.enums.YesEnum;
import com.mtd.iam.mapper.UserCredentialMapper;
import com.mtd.iam.mapper.UserMfaMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户凭证服务
 */
@Slf4j
@Service
public class UserCredentialService extends ServiceImpl<UserCredentialMapper, UserCredential> {

    @Autowired
    private UserCredentialMapper userCredentialMapper;

    @Autowired
    private UserMfaMapper userMfaMapper;

    /**
     * 验证用户密码
     */
    public boolean verifyPassword(User user, String rawPassword) {
        if (ObjUtil.isNull(user) || ObjUtil.isEmpty(rawPassword)) {
            return false;
        }

        // 查询用户凭证
        UserCredential credential = userCredentialMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<UserCredential>lambdaQuery()
                        .eq(UserCredential::getUserId, user.getId())
                        .orderByDesc(UserCredential::getId)
                        .last(StrConstant.LIMIT_1)
        );

        if (ObjUtil.isNull(credential) || ObjUtil.isEmpty(credential.getPassword())) {
            log.warn("用户没有设置密码，userId: {}", user.getId());
            return false;
        }

        // 使用 MD5 验证密码
        String hashedPassword = DigestUtil.md5Hex(rawPassword);
        boolean isValid = hashedPassword.equals(credential.getPassword());

        if (!isValid) {
            log.info("密码验证失败，userId: {}", user.getId());
        } else {
            log.debug("密码验证成功，userId: {}", user.getId());
        }

        return isValid;
    }

    /**
     * 检查用户是否启用了 MFA
     */
    public boolean isUserMfaEnabled(User user) {
        if (ObjUtil.isNull(user)) {
            return false;
        }

        UserMfa userMfa = userMfaMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<UserMfa>lambdaQuery()
                        .eq(UserMfa::getUserId, user.getId())
                        .eq(UserMfa::getStatus, YesEnum.YES.getCode()) // 1 表示启用
                        .last(StrConstant.LIMIT_1)
        );

        return ObjUtil.isNotNull(userMfa);
    }

    /**
     * 设置用户密码
     */
    public void setPassword(Long userId, String password) {
        String hashedPassword = DigestUtil.md5Hex(password);

        UserCredential credential = new UserCredential();
        credential.setUserId(userId);
        credential.setPassword(hashedPassword);
        userCredentialMapper.insert(credential);
        log.info("创建用户密码凭证，userId: {}", userId);

    }
}
