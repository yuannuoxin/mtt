package com.mtd.st.service;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.Result;
import com.mtd.common.core.result.ResultCode;
import com.mtd.st.enums.StatusEnum;
import com.mtd.st.mapper.SysPermissionMapper;
import com.mtd.st.mapper.SysRoleMapper;
import com.mtd.st.mapper.SysUserMapper;
import com.mtd.st.model.entity.SysPermission;
import com.mtd.st.model.entity.SysRole;
import com.mtd.st.model.entity.SysUser;
import com.mtd.st.model.entity.table.SysPermissionTableDef;
import com.mtd.st.model.entity.table.SysRolePermissionTableDef;
import com.mtd.st.model.entity.table.SysRoleTableDef;
import com.mtd.st.model.entity.table.SysUserRoleTableDef;
import com.mtd.st.model.req.EmailCodeLoginReq;
import com.mtd.st.model.req.PasswordLoginReq;
import com.mtd.st.model.resp.LoginResp;
import com.mtd.st.model.resp.PermissionResp;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private SysRoleMapper roleMapper;
    @Autowired
    private SysPermissionMapper permissionMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String EMAIL_CODE_PREFIX = "login:email:code:";
    private static final int CODE_EXPIRE_MINUTES = 5;

    public LoginResp passwordLogin(PasswordLoginReq req) {
        SysUser user = findUserByAccount(req.getAccount());

        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "账号或密码错误");
        }

        if (user.getStatus().equals(StatusEnum.DISABLED.getCode())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "账号已被禁用");
        }

        if (!BCrypt.checkpw(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "账号或密码错误");
        }

        return doLogin(user);
    }

    public LoginResp emailCodeLogin(EmailCodeLoginReq req) {
        String cacheKey = EMAIL_CODE_PREFIX + req.getEmail();
        String cachedCode = redisTemplate.opsForValue().get(cacheKey);

        if (cachedCode == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "验证码已过期");
        }

        if (!cachedCode.equals(req.getCode())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "验证码错误");
        }

        redisTemplate.delete(cacheKey);

        SysUser user = userMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq(SysUser::getEmail, req.getEmail())
        );

        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "该邮箱未注册");
        }

        if (user.getStatus().equals(StatusEnum.DISABLED.getCode())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "账号已被禁用");
        }

        return doLogin(user);
    }

    public void sendEmailCode(String email) {
        SysUser user = userMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq(SysUser::getEmail, email)
        );

        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "该邮箱未注册");
        }

        String code = RandomUtil.randomNumbers(6);
        String cacheKey = EMAIL_CODE_PREFIX + email;

        redisTemplate.opsForValue().set(cacheKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        log.info("邮箱验证码: {}, 邮箱: {}", code, email);
    }

    public void logout() {
        StpUtil.logout();
    }

    private LoginResp doLogin(SysUser user) {
        StpUtil.login(user.getId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        LoginResp resp = new LoginResp();
        resp.setTokenInfo(tokenInfo);
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());

        return resp;
    }

    public Result<PermissionResp> getPermissions() {
        Long userId = StpUtil.getLoginIdAsLong();
        PermissionResp permissionVo = new PermissionResp();

        List<String> roleCodes = getUserRoles(userId);
        List<String> permissionCodes = getUserPermissions(userId);

        permissionVo.setRoleCodes(roleCodes);
        permissionVo.setPermissionCodes(permissionCodes);
        return Result.success(permissionVo);
    }

    private SysUser findUserByAccount(String account) {
        return userMapper.selectOneByQuery(
                QueryWrapper.create()
                        .where(SysUser::getUsername).eq(account)
                        .or(SysUser::getEmail).eq(account)
                        .or(SysUser::getPhone).eq(account)
        );
    }

    private List<String> getUserRoles(Long userId) {
        SysRoleTableDef role = SysRoleTableDef.SYS_ROLE;
        SysUserRoleTableDef userRole = SysUserRoleTableDef.SYS_USER_ROLE;

        List<SysRole> roles = roleMapper.selectListByQuery(
                QueryWrapper.create()
                        .select(role.ROLE_CODE)
                        .from(role)
                        .innerJoin(userRole).on(role.ID.eq(userRole.ROLE_ID))
                        .where(userRole.USER_ID.eq(userId))
                        .and(role.STATUS.eq(StatusEnum.ENABLED.getCode()))
        );
        return roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList());
    }

    private List<String> getUserPermissions(Long userId) {
        SysPermissionTableDef perm = SysPermissionTableDef.SYS_PERMISSION;
        SysRolePermissionTableDef rolePerm = SysRolePermissionTableDef.SYS_ROLE_PERMISSION;
        SysUserRoleTableDef userRole = SysUserRoleTableDef.SYS_USER_ROLE;

        List<SysPermission> permissions = permissionMapper.selectListByQuery(
                QueryWrapper.create()
                        .select(perm.PERM_CODE)
                        .from(perm)
                        .innerJoin(rolePerm).on(perm.ID.eq(rolePerm.PERMISSION_ID))
                        .innerJoin(userRole).on(rolePerm.ROLE_ID.eq(userRole.ROLE_ID))
                        .where(userRole.USER_ID.eq(userId))
                        .and(perm.STATUS.eq(StatusEnum.ENABLED.getCode()))
        );
        return permissions.stream().map(SysPermission::getPermCode).collect(Collectors.toList());
    }
}
