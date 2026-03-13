package com.mtd.demo.request;

import com.mtd.common.core.request.ListRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询用户列表请求参数（不分页）
 * 返回所有符合条件的数据
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserListRequest extends ListRequest {

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
