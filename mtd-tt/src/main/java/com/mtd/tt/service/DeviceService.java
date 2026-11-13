package com.mtd.tt.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.enums.LoginProviderEnum;
import com.mtd.tt.model.resp.DeviceResp;
import com.mtd.tt.util.DeviceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 登录设备管理（类似 Telegram / 币安）：列出当前账号所有在线会话（一台设备一个 token），
 * 并支持远程下线指定设备或一键下线其他所有设备。
 * <p>
 * 设备资料存于该 token 的 Token-Session（登录时由 LoginLogService 写入），
 * 对外只暴露 token 的 SHA-256 短哈希（deviceId），不返回真实 token。
 */
@Slf4j
@Service
public class DeviceService {

    /** 当前账号的所有在线设备，当前设备标记 current=true，按登录时间倒序 */
    public List<DeviceResp> list(Long userId) {
        String currentToken = safeTokenValue();
        List<String> tokens = tokenList(userId, currentToken);
        List<DeviceResp> out = new ArrayList<>();
        for (String t : tokens) {
            DeviceResp d = toResp(t, currentToken);
            if (d != null) {
                out.add(d);
            }
        }
        out.sort(Comparator.comparing(DeviceResp::getLoginTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return out;
    }

    /**
     * 下线指定设备。
     *
     * @return true 表示被下线的是当前设备（前端应清除本地登录态）
     */
    public boolean kick(Long userId, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "设备 ID 不能为空");
        }
        String currentToken = safeTokenValue();
        for (String t : tokenList(userId, currentToken)) {
            if (deviceId.equals(DeviceUtil.deviceId(t))) {
                StpUtil.kickoutByTokenValue(t);
                return t.equals(currentToken);
            }
        }
        throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "设备不存在或已下线");
    }

    /** 下线除当前设备外的所有设备，返回下线条数 */
    public int kickOthers(Long userId) {
        String currentToken = safeTokenValue();
        int count = 0;
        for (String t : tokenList(userId, currentToken)) {
            if (t.equals(currentToken)) {
                continue;
            }
            try {
                StpUtil.kickoutByTokenValue(t);
                count++;
            } catch (Exception e) {
                log.warn("下线设备失败 userId={}: {}", userId, e.getMessage());
            }
        }
        return count;
    }

    /** 该账号的 token 列表；dao 不支持搜索时退化为仅当前 token */
    private List<String> tokenList(Long userId, String currentToken) {
        try {
            List<String> list = StpUtil.getTokenValueListByLoginId(userId);
            if (list != null && !list.isEmpty()) {
                return list;
            }
        } catch (Exception e) {
            log.warn("查询登录 token 列表失败 userId={}: {}", userId, e.getMessage());
        }
        return currentToken == null ? List.of() : List.of(currentToken);
    }

    private String safeTokenValue() {
        try {
            return StpUtil.getTokenValue();
        } catch (Exception e) {
            return null;
        }
    }

    private DeviceResp toResp(String token, String currentToken) {
        try {
            SaSession s = StpUtil.getTokenSessionByToken(token);
            if (s == null) {
                return null;
            }
            DeviceResp d = new DeviceResp();
            d.setDeviceId(DeviceUtil.deviceId(token));
            String ua = str(s.get(LoginLogService.SESSION_UA));
            String name = str(s.get(LoginLogService.SESSION_NAME));
            d.setName(name != null ? name : DeviceUtil.deviceName(ua));
            d.setBrowser(DeviceUtil.browserName(ua));
            d.setOs(DeviceUtil.osName(ua));
            d.setIp(str(s.get(LoginLogService.SESSION_IP)));
            String provider = str(s.get(LoginLogService.SESSION_PROVIDER));
            d.setProvider(provider);
            LoginProviderEnum p = LoginProviderEnum.of(provider);
            d.setProviderDesc(p == null ? provider : p.getDesc());
            d.setFingerprint(str(s.get(LoginLogService.SESSION_FP)));
            long createTime = s.getCreateTime();
            d.setLoginTime(createTime > 0
                    ? OffsetDateTime.ofInstant(Instant.ofEpochMilli(createTime), ZoneId.systemDefault())
                    : null);
            d.setCurrent(token.equals(currentToken));
            return d;
        } catch (Exception e) {
            log.warn("读取设备会话失败: {}", e.getMessage());
            return null;
        }
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
