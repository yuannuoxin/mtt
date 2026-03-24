package com.mtd.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtd.iam.entity.UserVerifyCode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 短信/邮箱验证码 Mapper 接口
 */
@Mapper
public interface UserVerifyCodeMapper extends BaseMapper<UserVerifyCode> {
    
}
