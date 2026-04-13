package com.mtd.st.service;

import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.ResultCode;
import com.mtd.st.enums.StatusEnum;
import com.mtd.st.mapper.SysRoleMapper;
import com.mtd.st.mapper.SysRolePermissionMapper;
import com.mtd.st.model.entity.SysRole;
import com.mtd.st.model.entity.SysRolePermission;
import com.mtd.st.model.entity.table.SysRolePermissionTableDef;
import com.mtd.st.model.entity.table.SysRoleTableDef;
import com.mtd.st.model.req.AssignPermissionReq;
import com.mtd.st.model.req.RoleCreateReq;
import com.mtd.st.model.req.RolePageReq;
import com.mtd.st.model.req.RoleUpdateReq;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    public Page<SysRole> page(RolePageReq req) {
        Page<SysRole> page = new Page<>(req.getPageNum(), req.getPageSize());
        QueryWrapper query = QueryWrapper.create();

        if (req.getRoleCode() != null && !req.getRoleCode().isEmpty()) {
            query.and(SysRoleTableDef.SYS_ROLE.ROLE_CODE.like(req.getRoleCode()));
        }
        if (req.getRoleName() != null && !req.getRoleName().isEmpty()) {
            query.and(SysRoleTableDef.SYS_ROLE.ROLE_NAME.like(req.getRoleName()));
        }
        if (req.getStatus() != null) {
            query.and(SysRoleTableDef.SYS_ROLE.STATUS.eq(req.getStatus()));
        }

        return roleMapper.paginate(page, query);
    }

    public List<SysRole> list() {
        return roleMapper.selectAll();
    }

    public SysRole getById(Long id) {
        return roleMapper.selectOneById(id);
    }

    @Transactional
    public void create(RoleCreateReq req) {
        SysRole existRole = roleMapper.selectOneByQuery(
                QueryWrapper.create()
                        .where(SysRoleTableDef.SYS_ROLE.ROLE_CODE.eq(req.getRoleCode()))
        );
        if (existRole != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "角色编码已存在");
        }

        SysRole role = new SysRole();
        role.setRoleCode(req.getRoleCode());
        role.setRoleName(req.getRoleName());
        role.setDescription(req.getDescription());
        role.setSort(req.getSort() != null ? req.getSort() : 0);
        role.setStatus(req.getStatus() != null ? req.getStatus() : StatusEnum.ENABLED.getCode());

        roleMapper.insert(role);
    }

    @Transactional
    public void update(RoleUpdateReq req) {
        SysRole role = roleMapper.selectOneById(req.getId());
        if (role == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "角色不存在");
        }

        role.setRoleName(req.getRoleName());
        role.setDescription(req.getDescription());
        if (req.getSort() != null) {
            role.setSort(req.getSort());
        }
        if (req.getStatus() != null) {
            role.setStatus(req.getStatus());
        }

        roleMapper.update(role);
    }

    @Transactional
    public void delete(Long id) {
        roleMapper.deleteById(id);
    }

    @Transactional
    public void assignPermissions(AssignPermissionReq req) {
        List<Long> existingPermIds = Objects.requireNonNull(rolePermissionMapper.selectListByQuery(
                QueryWrapper.create()
                        .select(SysRolePermissionTableDef.SYS_ROLE_PERMISSION.PERMISSION_ID)
                        .where(SysRolePermissionTableDef.SYS_ROLE_PERMISSION.ROLE_ID.eq(req.getRoleId()))
        )).stream().map(SysRolePermission::getPermissionId).toList();

        List<Long> newPermIds = req.getPermissionIds();

        List<Long> toDelete = existingPermIds.stream()
                .filter(id -> !newPermIds.contains(id))
                .collect(java.util.stream.Collectors.toList());

        List<Long> toAdd = newPermIds.stream()
                .filter(id -> !existingPermIds.contains(id))
                .toList();

        if (!toDelete.isEmpty()) {
            rolePermissionMapper.deleteByQuery(
                    QueryWrapper.create()
                            .where(SysRolePermissionTableDef.SYS_ROLE_PERMISSION.ROLE_ID.eq(req.getRoleId()))
                            .and(SysRolePermissionTableDef.SYS_ROLE_PERMISSION.PERMISSION_ID.in(toDelete))
            );
        }

        for (Long permissionId : toAdd) {
            SysRolePermission rolePermission = new SysRolePermission();
            rolePermission.setRoleId(req.getRoleId());
            rolePermission.setPermissionId(permissionId);
            rolePermissionMapper.insert(rolePermission);
        }
    }
}
