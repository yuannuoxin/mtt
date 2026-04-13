package com.mtd.st.service;

import cn.hutool.crypto.digest.BCrypt;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.ResultCode;
import com.mtd.st.enums.StatusEnum;
import com.mtd.st.mapper.SysUserMapper;
import com.mtd.st.mapper.SysUserRoleMapper;
import com.mtd.st.model.entity.SysUser;
import com.mtd.st.model.entity.SysUserRole;
import com.mtd.st.model.entity.table.SysUserRoleTableDef;
import com.mtd.st.model.entity.table.SysUserTableDef;
import com.mtd.st.model.req.AssignRoleReq;
import com.mtd.st.model.req.UserCreateReq;
import com.mtd.st.model.req.UserPageReq;
import com.mtd.st.model.req.UserUpdateReq;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;


@Service
public class UserService extends ServiceImpl<SysUserMapper, SysUser> {

    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private SysUserRoleMapper userRoleMapper;

    public Page<SysUser> page(UserPageReq req) {
        Page<SysUser> page = new Page<>(req.getPageNum(), req.getPageSize());
        QueryWrapper query = QueryWrapper.create();

        if (req.getUsername() != null && !req.getUsername().isEmpty()) {
            query.and(SysUserTableDef.SYS_USER.USERNAME.like(req.getUsername()));
        }
        if (req.getNickname() != null && !req.getNickname().isEmpty()) {
            query.and(SysUserTableDef.SYS_USER.NICKNAME.like(req.getNickname()));
        }
        if (req.getEmail() != null && !req.getEmail().isEmpty()) {
            query.and(SysUserTableDef.SYS_USER.EMAIL.like(req.getEmail()));
        }
        if (req.getPhone() != null && !req.getPhone().isEmpty()) {
            query.and(SysUserTableDef.SYS_USER.PHONE.like(req.getPhone()));
        }
        if (req.getStatus() != null) {
            query.and(SysUserTableDef.SYS_USER.STATUS.eq(req.getStatus()));
        }

        return userMapper.paginate(page, query);
    }

    public List<SysUser> list() {
        return userMapper.selectAll();
    }

    public SysUser getById(Long id) {
        return userMapper.selectOneById(id);
    }

    @Transactional
    public void create(UserCreateReq req) {
        SysUser existUser = userMapper.selectOneByQuery(
                QueryWrapper.create()
                        .where(SysUserTableDef.SYS_USER.USERNAME.eq(req.getUsername()))
        );
        if (existUser != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(BCrypt.hashpw(req.getPassword()));
        user.setNickname(req.getNickname());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setAvatar(req.getAvatar());
        user.setStatus(req.getStatus() != null ? req.getStatus() : StatusEnum.ENABLED.getCode());

        userMapper.insert(user);
    }

    @Transactional
    public void update(UserUpdateReq req) {
        SysUser user = userMapper.selectOneById(req.getId());
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户不存在");
        }

        user.setNickname(req.getNickname());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setAvatar(req.getAvatar());
        if (req.getStatus() != null) {
            user.setStatus(req.getStatus());
        }

        userMapper.update(user);
    }

    @Transactional
    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    @Transactional
    public void assignRoles(AssignRoleReq req) {
        List<Long> existingRoleIds = Objects.requireNonNull(userRoleMapper.selectListByQuery(
                QueryWrapper.create()
                        .select(SysUserRoleTableDef.SYS_USER_ROLE.ROLE_ID)
                        .where(SysUserRoleTableDef.SYS_USER_ROLE.USER_ID.eq(req.getUserId()))
        )).stream().map(SysUserRole::getRoleId).toList();

        List<Long> newRoleIds = req.getRoleIds();

        // 需要删除的角色ID
        List<Long> toDelete = existingRoleIds.stream()
                .filter(id -> !newRoleIds.contains(id))
                .collect(java.util.stream.Collectors.toList());

        // 需要新增的角色ID
        List<Long> toAdd = newRoleIds.stream()
                .filter(id -> !existingRoleIds.contains(id))
                .toList();

        // 删除不再关联的角色
        if (!toDelete.isEmpty()) {
            userRoleMapper.deleteByQuery(
                    QueryWrapper.create()
                            .where(SysUserRoleTableDef.SYS_USER_ROLE.USER_ID.eq(req.getUserId()))
                            .and(SysUserRoleTableDef.SYS_USER_ROLE.ROLE_ID.in(toDelete))
            );
        }

        // 新增角色关联
        for (Long roleId : toAdd) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(req.getUserId());
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
    }
}
