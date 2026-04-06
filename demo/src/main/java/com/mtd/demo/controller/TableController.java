package com.mtd.demo.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtd.common.core.result.Result;
import com.mtd.demo.constant.StrConstant;
import com.mtd.demo.request.TableQueryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 通用表查询控制器
 */
@Tag(name = "通用表查询", description = "所有表的通用查询接口")
@RestController
@RequestMapping("/table")
public class TableController {

    @Autowired
    private ApplicationContext applicationContext;


    // 白名单：允许查询的 Mapper Bean 名称
    private static final Set<String> ALLOWED_MAPPERS = new HashSet<>();

    static {
        ALLOWED_MAPPERS.add("userMapper");
        ALLOWED_MAPPERS.add("sysConfigMapper");
    }

    @Operation(summary = "【通用】任意表分页查询")
    @PostMapping("/query")
    public Result<IPage<?>> query2(@RequestBody TableQueryRequest request) {
        String mapperName = request.getMapper();

        // 1. 验证 Mapper 名称
        if (!ALLOWED_MAPPERS.contains(mapperName)) {
            return Result.error("不支持的 Mapper: " + mapperName);
        }

        // 2. 获取对应的 BaseMapper
        BaseMapper<?> baseMapper = (BaseMapper<?>) applicationContext.getBean(mapperName);

        // 3. 构建分页对象
        Page<Object> page = new Page<>(request.getPageNum(), request.getPageSize());

        // =======================
        // 3. 后端自己构建 Wrapper
        // =======================
        QueryWrapper<Object> wrapper = new QueryWrapper<>();

        // 等值
        if (request.getEq() != null) wrapper.allEq(request.getEq());
        // 模糊
        if (request.getLike() != null) request.getLike().forEach(wrapper::like);
        // 大于
        if (request.getGt() != null) request.getGt().forEach(wrapper::gt);
        // 小于
        if (request.getLt() != null) request.getLt().forEach(wrapper::lt);

        // ====================== ✅ 排序
        if (StrUtil.isNotBlank(request.getSortField())) {
            if (StrConstant.DESC.equalsIgnoreCase(request.getSortOrder())) {
                wrapper.orderByDesc(request.getSortField());
            } else {
                wrapper.orderByAsc(request.getSortField());
            }
        }

        // ====================== ✅ 多字段排序（核心）
        if (request.getSort() != null && !request.getSort().isEmpty()) {
            for (Map<String, String> s : request.getSort()) {
                String field = s.get("field");
                String order = s.get("order");
                if (StrUtil.isBlank(field)) continue;

                if (StrConstant.DESC.equalsIgnoreCase(order)) {
                    wrapper.orderByDesc(field);
                } else {
                    wrapper.orderByAsc(field);
                }
            }
        }

        // 4. 执行查询（强制转换解决泛型问题）
        @SuppressWarnings("unchecked")
        IPage<?> resultPage = ((BaseMapper<Object>) baseMapper).selectPage(page, (QueryWrapper<Object>) wrapper);

        return Result.success(resultPage);
    }
}
