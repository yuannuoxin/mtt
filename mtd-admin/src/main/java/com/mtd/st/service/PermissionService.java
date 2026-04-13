package com.mtd.st.service;

import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.ResultCode;
import com.mtd.st.model.entity.SysPermission;
import com.mtd.st.enums.StatusEnum;
import com.mtd.st.mapper.SysPermissionMapper;
import com.mtd.st.model.entity.table.SysPermissionTableDef;
import com.mtd.st.model.req.PermissionCreateReq;
import com.mtd.st.model.req.PermissionPageReq;
import com.mtd.st.model.req.PermissionUpdateReq;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissionService {

    @Autowired
    private SysPermissionMapper permissionMapper;

    public Page<SysPermission> page(PermissionPageReq req) {
        Page<SysPermission> page = new Page<>(req.getPageNum(), req.getPageSize());
        QueryWrapper query = QueryWrapper.create();

        if (req.getPermCode() != null && !req.getPermCode().isEmpty()) {
            query.and(SysPermissionTableDef.SYS_PERMISSION.PERM_CODE.like(req.getPermCode()));
        }
        if (req.getPermName() != null && !req.getPermName().isEmpty()) {
            query.and(SysPermissionTableDef.SYS_PERMISSION.PERM_NAME.like(req.getPermName()));
        }
        if (req.getPermType() != null) {
            query.and(SysPermissionTableDef.SYS_PERMISSION.PERM_TYPE.eq(req.getPermType()));
        }
        if (req.getStatus() != null) {
            query.and(SysPermissionTableDef.SYS_PERMISSION.STATUS.eq(req.getStatus()));
        }

        return permissionMapper.paginate(page, query);
    }

    public List<SysPermission> list() {
        return permissionMapper.selectAll();
    }

    public SysPermission getById(Long id) {
        return permissionMapper.selectOneById(id);
    }

    public List<SysPermission> listByParentId(Long parentId) {
        return permissionMapper.selectListByQuery(
                QueryWrapper.create()
                        .where(SysPermissionTableDef.SYS_PERMISSION.PARENT_ID.eq(parentId != null ? parentId : 0))
        );
    }

    @Transactional
    public void create(PermissionCreateReq req) {
        SysPermission existPerm = permissionMapper.selectOneByQuery(
                QueryWrapper.create()
                        .where(SysPermissionTableDef.SYS_PERMISSION.PERM_CODE.eq(req.getPermCode()))
        );
        if (existPerm != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "权限编码已存在");
        }

        SysPermission permission = new SysPermission();
        permission.setParentId(req.getParentId() != null ? req.getParentId() : 0L);
        permission.setPermCode(req.getPermCode());
        permission.setPermName(req.getPermName());
        permission.setPermType(req.getPermType());
        permission.setPath(req.getPath());
        permission.setComponent(req.getComponent());
        permission.setIcon(req.getIcon());
        permission.setSort(req.getSort() != null ? req.getSort() : 0);
        permission.setStatus(req.getStatus() != null ? req.getStatus() : StatusEnum.ENABLED.getCode());

        permissionMapper.insert(permission);
    }

    @Transactional
    public void update(PermissionUpdateReq req) {
        SysPermission permission = permissionMapper.selectOneById(req.getId());
        if (permission == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "权限不存在");
        }

        permission.setPermName(req.getPermName());
        permission.setPath(req.getPath());
        permission.setComponent(req.getComponent());
        permission.setIcon(req.getIcon());
        if (req.getSort() != null) {
            permission.setSort(req.getSort());
        }
        if (req.getStatus() != null) {
            permission.setStatus(req.getStatus());
        }

        permissionMapper.update(permission);
    }

    @Transactional
    public void delete(Long id) {
        permissionMapper.deleteById(id);
    }
}
