package com.mtd.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtd.iam.entity.UserAuthThird;
import org.apache.ibatis.annotations.Mapper;

/**
 * 第三方登录绑定 Mapper 接口
 */
@Mapper
public interface UserAuthThirdMapper extends BaseMapper<UserAuthThird> {
    
}
