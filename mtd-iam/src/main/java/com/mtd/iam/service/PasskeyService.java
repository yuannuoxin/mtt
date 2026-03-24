package com.mtd.iam.service;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.codec.Base64Encoder;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.ResultCode;
import com.mtd.iam.entity.User;
import com.mtd.iam.entity.UserPasskey;
import com.mtd.iam.enums.YesEnum;
import com.mtd.iam.mapper.UserMapper;
import com.mtd.iam.mapper.UserPasskeyMapper;
import com.mtd.iam.req.PasskeyLoginRequest;
import com.mtd.iam.req.PasskeyRegisterRequest;
import com.mtd.iam.req.PasskeyChallengeRequest;
import com.mtd.iam.resp.*;
import com.mtd.iam.util.PasskeyHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Passkey 认证服务
 */
@Slf4j
@Service
public class PasskeyService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserPasskeyMapper userPasskeyMapper;

    /**
     * 生成注册挑战值
     *
     * @param req 请求参数
     * @return 挑战值和用户信息
     */
    public PasskeyChallengeResp generateRegisterChallenge(PasskeyChallengeRequest req) {
        log.info("生成 Passkey 注册挑战值，rpId: {}", req.getRpId());

        // 1. 获取当前登录用户
        long loginId = StpUtil.getLoginIdAsLong();
        
        // 2. 查询用户是否存在
        User user = userMapper.selectById(loginId);

        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户不存在");
        }

        // 3. 生成随机挑战值
        byte[] challenge = PasskeyHelper.generateChallenge();

        // 4. 构建响应对象（使用前端传入的 RpId）
        PasskeyChallengeResp result = new PasskeyChallengeResp();
        result.setChallenge(Base64Encoder.encodeUrlSafe(challenge));
        result.setRpId(req.getRpId());
        result.setRpName("MTD IAM");
        result.setUserId(Base64Encoder.encodeUrlSafe(String.valueOf(loginId).getBytes()));
        result.setUserName(user.getUsername());
        result.setUserDisplayName(user.getNickname());

        log.info("Passkey 注册挑战值生成成功，userId: {}, rpId: {}", user.getId(), req.getRpId());
        return result;
    }

    /**
     * 完成 Passkey 注册
     *
     * @param req 注册请求参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeRegister(PasskeyRegisterRequest req) {
        log.info("完成 Passkey 注册，credentialId: {}", req.getCredentialId());

        try {
            // 1. 检查凭证 ID 是否已存在
            LambdaQueryWrapper<UserPasskey> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserPasskey::getCredentialId, req.getCredentialId());
            UserPasskey existingPasskey = userPasskeyMapper.selectOne(wrapper);

            if (existingPasskey != null) {
                log.warn("凭证 ID 已存在：{}", req.getCredentialId());
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "该设备已注册过 Passkey");
            }

            // 2. 获取当前登录用户的 ID（需要先通过其他方式登录）
            Long userId = StpUtil.getLoginIdAsLong();

            // 3. 保存 Passkey 到数据库
            UserPasskey passkey = new UserPasskey();
            passkey.setUserId(userId);
            passkey.setCredentialId(req.getCredentialId());
            passkey.setPublicKey(req.getPublicKey());
            passkey.setDeviceName(StrUtil.emptyToDefault(req.getDeviceName(), "未知设备"));
            passkey.setTransports(req.getTransports());
            passkey.setStatus(YesEnum.YES.getCode());

            userPasskeyMapper.insert(passkey);

            log.info("Passkey 注册成功，userId: {}, credentialId: {}", userId, req.getCredentialId());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Passkey 注册失败", e);
            throw new BusinessException(ResultCode.ERROR.getCode(), "Passkey 注册失败：" + e.getMessage());
        }
    }

    /**
     * 生成登录挑战值
     *
     * @param req 请求参数
     * @return 挑战值
     */
    public PasskeyChallengeResp generateLoginChallenge(PasskeyChallengeRequest req) {
        log.info("生成 Passkey 登录挑战值，rpId: {}", req.getRpId());

        // 生成随机挑战值
        byte[] challenge = PasskeyHelper.generateChallenge();

        PasskeyChallengeResp result = new PasskeyChallengeResp();
        result.setChallenge(Base64Encoder.encodeUrlSafe(challenge));
        result.setRpId(req.getRpId());

        log.info("Passkey 登录挑战值生成成功，rpId: {}", req.getRpId());
        return result;
    }

    /**
     * 完成 Passkey 登录
     *
     * @param req 登录请求参数
     * @return Token 信息
     */
    @Transactional(rollbackFor = Exception.class)
    public SaTokenInfo completeLogin(PasskeyLoginRequest req) {
        log.info("完成 Passkey 登录，credentialId: {}", req.getCredentialId());

        try {
            // 1. 查询 Passkey 信息
            LambdaQueryWrapper<UserPasskey> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserPasskey::getCredentialId, req.getCredentialId());
            wrapper.eq(UserPasskey::getStatus, YesEnum.YES.getCode());
            UserPasskey passkey = userPasskeyMapper.selectOne(wrapper);

            if (passkey == null) {
                log.warn("Passkey 不存在或已禁用：{}", req.getCredentialId());
                throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Passkey 无效或已被禁用");
            }

            // 2. 验证签名
            boolean verified = PasskeyHelper.verifySignature(
                    passkey.getPublicKey(),
                    req.getAuthenticatorData(),
                    req.getClientDataJSON(),
                    req.getSignature()
            );

            if (!verified) {
                log.warn("Passkey 签名验证失败");
                throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "签名验证失败");
            }

            // 3. 查询用户信息
            User user = userMapper.selectById(passkey.getUserId());
            if (user == null) {
                log.warn("用户不存在：userId: {}", passkey.getUserId());
                throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户不存在");
            }

            // 4. 检查用户状态
            if (!YesEnum.YES.getCode().equals(user.getStatus())) {
                log.warn("用户状态异常：userId: {}, status: {}", user.getId(), user.getStatus());
                throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "用户状态异常");
            }

            // 5. 登录成功，生成 Token
            StpUtil.login(user.getId());
            var tokenInfo = StpUtil.getTokenInfo();

            log.info("Passkey 登录成功，userId: {}, username: {}", user.getId(), user.getUsername());

            return tokenInfo;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Passkey 登录失败", e);
            throw new BusinessException(ResultCode.ERROR.getCode(), "Passkey 登录失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户的 Passkey 列表
     *
     * @return Passkey 列表
     */
    public PasskeyListResp getUserPasskeys() {
        Long userId = StpUtil.getLoginIdAsLong();

        LambdaQueryWrapper<UserPasskey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPasskey::getUserId, userId);

        List<UserPasskey> passkeys = userPasskeyMapper.selectList(wrapper);

        // 转换为响应对象
        List<PasskeyItemResp> items = new ArrayList<>();
        for (UserPasskey pk : passkeys) {
            PasskeyItemResp item = new PasskeyItemResp();
            item.setId(pk.getId());
            item.setCredentialId(pk.getCredentialId());
            item.setDeviceName(pk.getDeviceName());
            item.setTransports(pk.getTransports());
            item.setCreateTime(pk.getCreateTime());
            items.add(item);
        }

        PasskeyListResp result = new PasskeyListResp();
        result.setCount(items.size());
        result.setPasskeys(items);

        return result;
    }

    /**
     * 删除 Passkey
     *
     * @param credentialId 凭证 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePasskey(String credentialId) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 查询 Passkey
        LambdaQueryWrapper<UserPasskey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPasskey::getCredentialId, credentialId);
        wrapper.eq(UserPasskey::getUserId, userId);
        UserPasskey passkey = userPasskeyMapper.selectOne(wrapper);

        if (passkey == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "Passkey 不存在");
        }

        userPasskeyMapper.deleteById(passkey);

        log.info("Passkey 删除成功，userId: {}, credentialId: {}", userId, credentialId);
    }
}
