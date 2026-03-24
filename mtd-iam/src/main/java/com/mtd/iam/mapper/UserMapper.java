package com.mtd.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtd.iam.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户信息 Mapper 接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
}
