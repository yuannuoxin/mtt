package com.mtd.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtd.iam.entity.UserAccessLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户登录&访问日志 Mapper 接口
 */
@Mapper
public interface UserAccessLogMapper extends BaseMapper<UserAccessLog> {
    
}
