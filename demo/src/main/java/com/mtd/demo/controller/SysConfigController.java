package com.mtd.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.request.IdRequest;
import com.mtd.common.core.response.PageResponse;
import com.mtd.common.core.result.Result;
import com.mtd.common.core.result.ResultCode;
import com.mtd.common.mybatis.base.BaseEntity;
import com.mtd.demo.entity.SysConfig;
import com.mtd.demo.request.SysConfigPageRequest;
import com.mtd.demo.request.SysConfigSaveRequest;
import com.mtd.demo.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置控制器
 */
@Slf4j
@Tag(name = "系统配置管理", description = "系统公共配置相关接口")
@RestController
@RequestMapping("/sys/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @Operation(summary = "新增系统配置", description = "创建新的系统配置项")
    @PostMapping("/save")
    public Result<Void> save(@RequestBody SysConfigSaveRequest request) {
        // 1. 检查配置键是否已存在
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, request.getConfigKey());
        if (sysConfigService.count(wrapper) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "配置键已存在: " + request.getConfigKey());
        }

        // 2. 创建配置对象
        SysConfig config = new SysConfig();
        config.setConfigKey(request.getConfigKey());
        config.setConfigName(request.getConfigName());
        config.setJsonVal(request.getJsonVal());

        // 3. 保存到数据库
        boolean success = sysConfigService.save(config);
        if (!success) {
            throw new BusinessException(ResultCode.ERROR.getCode(), "保存配置失败");
        }

        return Result.success();
    }

    @Operation(summary = "更新系统配置", description = "根据 ID 更新系统配置项")
    @PostMapping("/update")
    public Result<Void> update(@RequestBody SysConfigSaveRequest request) {
        // 1. 参数校验
        if (request.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "ID 不能为空");
        }

        // 2. 检查配置是否存在
        SysConfig existConfig = sysConfigService.getById(request.getId());
        if (existConfig == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "配置不存在");
        }

        // 3. 检查配置键是否被其他配置占用
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, request.getConfigKey())
                .ne(SysConfig::getId, request.getId());
        if (sysConfigService.count(wrapper) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "配置键已被其他配置使用: " + request.getConfigKey());
        }

        // 4. 更新配置
        existConfig.setConfigKey(request.getConfigKey());
        existConfig.setConfigName(request.getConfigName());
        existConfig.setJsonVal(request.getJsonVal());

        boolean success = sysConfigService.updateById(existConfig);
        if (!success) {
            throw new BusinessException(ResultCode.ERROR.getCode(), "更新配置失败");
        }

        return Result.success();
    }

    @Operation(summary = "删除系统配置", description = "根据 ID 删除系统配置（逻辑删除）")
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody IdRequest request) {
        // 1. 检查配置是否存在
        SysConfig config = sysConfigService.getById(request.getId());
        if (config == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "配置不存在");
        }

        // 2. 逻辑删除
        boolean success = sysConfigService.removeById(request.getId());
        if (!success) {
            throw new BusinessException(ResultCode.ERROR.getCode(), "删除配置失败");
        }

        return Result.success();
    }

    @Operation(summary = "根据 ID 查询配置", description = "通过配置 ID 获取配置详细信息")
    @PostMapping("/get")
    public Result<SysConfig> get(@RequestBody IdRequest request) {
        SysConfig config = sysConfigService.getById(request.getId());
        if (config == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(config);
    }

    @Operation(summary = "根据配置键查询配置", description = "通过配置键获取配置信息")
    @PostMapping("/getByKey")
    public Result<SysConfig> getByKey(@RequestBody SysConfigSaveRequest request) {
        if (StringUtils.isBlank(request.getConfigKey())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "配置键不能为空");
        }

        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, request.getConfigKey());
        SysConfig config = sysConfigService.getOne(wrapper);

        if (config == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(config);
    }

    @Operation(summary = "分页查询配置列表", description = "分页查询系统配置列表")
    @PostMapping("/page")
    public Result<PageResponse<SysConfig>> page(@RequestBody SysConfigPageRequest request) {
        // 1. 构建查询条件
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();

        // 配置键模糊查询
        if (StringUtils.isNotBlank(request.getConfigKey())) {
            wrapper.eq(SysConfig::getConfigKey, request.getConfigKey());
        }

        // 配置名称模糊查询
        if (StringUtils.isNotBlank(request.getConfigName())) {
            wrapper.eq(SysConfig::getConfigName, request.getConfigName());
        }

        // 按 ID 降序排列
        wrapper.orderByDesc(BaseEntity::getId);

        // 2. 执行分页查询
        Page<SysConfig> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<SysConfig> resultPage = sysConfigService.page(page, wrapper);

        // 3. 转换为 PageResponse
        PageResponse<SysConfig> response = new PageResponse<>();
        response.setList(resultPage.getRecords());
        response.setTotal(resultPage.getTotal());
        response.setPageNum((int) resultPage.getCurrent());
        response.setPageSize((int) resultPage.getSize());
        response.setPages((int) resultPage.getPages());

        return Result.success(response);
    }

    @Operation(summary = "查询所有配置列表")
    @PostMapping("/listAll")
    public Result<List<SysConfig>> listAll(@RequestBody SysConfigPageRequest request) {
        // 1. 构建查询条件
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();

        // 配置键模糊查询
        if (StringUtils.isNotBlank(request.getConfigKey())) {
            wrapper.eq(SysConfig::getConfigKey, request.getConfigKey());
        }

        // 配置名称模糊查询
        if (StringUtils.isNotBlank(request.getConfigName())) {
            wrapper.eq(SysConfig::getConfigName, request.getConfigName());
        }

        // 按 ID 降序排列
        wrapper.orderByDesc(BaseEntity::getId);

        // 2. 查询所有数据
        Page<SysConfig> page = new Page<>(request.getPageNum(), request.getPageSize());
        List<SysConfig> list = sysConfigService.list(page, wrapper);

        return Result.success(list);
    }

    @Operation(summary = "查询所有配置列表2")
    @PostMapping("/listAll2")
    public Result<?> listAll2(@RequestBody SysConfigPageRequest request) {
        // 1. 构建查询条件
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();

        // 配置键模糊查询
        if (StringUtils.isNotBlank(request.getConfigKey())) {
            wrapper.eq(SysConfig::getConfigKey, request.getConfigKey());
        }

        // 配置名称模糊查询
        if (StringUtils.isNotBlank(request.getConfigName())) {
            wrapper.eq(SysConfig::getConfigName, request.getConfigName());
        }

        // 按 ID 降序排列
        wrapper.orderByDesc(BaseEntity::getId);

        // 2. 查询所有数据
        Page<SysConfig> page = new Page<>(request.getPageNum(), request.getPageSize());
        List<SysConfig> list = sysConfigService.list(page, wrapper);
        Map<String, Object> listToMap = list.stream().collect(Collectors.toMap(SysConfig::getConfigKey, SysConfig::getJsonVal, (k1, k2) -> k1));

        return Result.success(listToMap);
    }
}
