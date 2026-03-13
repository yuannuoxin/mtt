package com.mtd.demo.request;

import com.mtd.common.core.request.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询用户分页请求参数
 * 返回分页数据，包含 pageNum、pageSize 等信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageRequest extends PageRequest {

    /**
     * 用户名（模糊查询）
     */
    private String username;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 状态：0-禁用 1-正常
     */
    private Integer status;
}
