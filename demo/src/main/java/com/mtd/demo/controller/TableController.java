package com.mtd.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mtd.common.core.result.Result;
import com.mtd.demo.entity.SysConfig;
import com.mtd.demo.entity.User;
import com.mtd.demo.mapper.SysConfigMapper;
import com.mtd.demo.request.TableQueryRequest;
import com.mtd.demo.request.TableSysConfigQueryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用表查询控制器
 */
@Tag(name = "通用表查询", description = "所有表的通用查询接口")
@RestController
@RequestMapping("/table")
public class TableController {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Resource
    private SqlSessionFactory sqlSessionFactory;

    @PostMapping("/querySysConfig")
    public Result<?> querySysConfig(@RequestBody TableSysConfigQueryRequest request) {

        // 3. 构建分页对象
        Page<SysConfig> page = new Page<>(request.getPageNum(), request.getPageSize());

        // 5. 执行查询
        Page<SysConfig> sysConfigPage = sysConfigMapper.selectPage(page, request.getQuery());

        return Result.success(sysConfigPage);
    }


    private static final Map<String, Class<?>> MAP = new HashMap<>();

    static {
        // 在这里注册你的表 <表名, 实体类>
        MAP.put("sys_user", User.class);
        MAP.put("mtd_sys_config", SysConfig.class);
    }

    @Operation(summary = "【通用】任意表分页查询")
    @PostMapping("/query")
    public Result<?> query(@RequestBody TableQueryRequest request) {
        String tableName = request.getTableName();
        Class<?> entityClass = MAP.get(tableName);

        if (entityClass == null) {
            return Result.error("表不存在");
        }

        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            // 调用泛型方法，彻底解决类型不匹配问题
            IPage<?> result = execute(entityClass, request, sqlSession);
            return Result.success(result);
        }
    }

    /**
     * 泛型方法：解决类型边界问题
     */
    private <T> IPage<T> execute(Class<T> entityClass, TableQueryRequest request, SqlSession sqlSession) {
        BaseMapper<T> mapper = SqlHelper.getMapper(entityClass, sqlSession);

        // 类型安全的 Page
        Page<T> page = new Page<>(request.getPageNum(), request.getPageSize());

        // 这里强制转换一次即可，无报错
        QueryWrapper<T> wrapper = (QueryWrapper<T>) request.getQuery();

        return mapper.selectPage(page, wrapper);
    }
}
