package com.mtd.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtd.iam.entity.UserPasskey;
import org.apache.ibatis.annotations.Mapper;

/**
 * Passkey/WebAuthn Mapper 接口
 */
@Mapper
public interface UserPasskeyMapper extends BaseMapper<UserPasskey> {
    
}
