package com.mtd.mfl.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mtd.mfl.entity.SysUser;
import com.mtd.mfl.mapper.SysUserMapper;
import com.mtd.mfl.service.SysUserService;
import org.springframework.stereotype.Service;

/**
 * 用户表 服务层实现。
 *
 * @author user
 * @since 2026-04-07
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>  implements SysUserService{

}
