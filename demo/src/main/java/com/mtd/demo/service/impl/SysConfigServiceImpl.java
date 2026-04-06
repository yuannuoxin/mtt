package com.mtd.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mtd.demo.entity.SysConfig;
import com.mtd.demo.mapper.SysConfigMapper;
import com.mtd.demo.service.SysConfigService;
import org.springframework.stereotype.Service;

/**
 * 系统配置服务实现类
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {
}
