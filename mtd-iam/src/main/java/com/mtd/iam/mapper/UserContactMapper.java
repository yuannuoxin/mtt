package com.mtd.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtd.iam.entity.UserContact;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户联系方式 Mapper 接口
 */
@Mapper
public interface UserContactMapper extends BaseMapper<UserContact> {
    
}
