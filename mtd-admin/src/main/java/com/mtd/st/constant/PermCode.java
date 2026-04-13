package com.mtd.st.constant;

public final class PermCode {
    // 用户管理
    public static final String USER_VIEW = "system:user:view";
    public static final String USER_CREATE = "system:user:create";
    public static final String USER_UPDATE = "system:user:update";
    public static final String USER_DELETE = "system:user:delete";
    public static final String USER_ASSIGN_ROLE = "system:user:assignRole";

    // 角色管理
    public static final String ROLE_VIEW = "system:role:view";
    public static final String ROLE_CREATE = "system:role:create";
    public static final String ROLE_UPDATE = "system:role:update";
    public static final String ROLE_DELETE = "system:role:delete";
    public static final String ROLE_ASSIGN_PERM = "system:role:assignPerm";

    // 权限管理
    public static final String PERM_VIEW = "system:perm:view";
    public static final String PERM_CREATE = "system:perm:create";
    public static final String PERM_UPDATE = "system:perm:update";
    public static final String PERM_DELETE = "system:perm:delete";
}
