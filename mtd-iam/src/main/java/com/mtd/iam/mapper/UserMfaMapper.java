package com.mtd.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtd.iam.entity.UserMfa;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMfaMapper extends BaseMapper<UserMfa> {
}
