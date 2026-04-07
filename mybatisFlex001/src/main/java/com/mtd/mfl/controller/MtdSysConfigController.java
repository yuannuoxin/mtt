package com.mtd.mfl.controller;

import com.mtd.common.core.result.Result;
import com.mtd.mfl.entity.MtdSysConfig;
import com.mtd.mfl.service.MtdSysConfigService;
import com.mybatisflex.core.paginate.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统公共配置表 控制层。
 *
 * @author user
 * @since 2026-04-07
 */
@RestController
@RequestMapping("/mtdSysConfig")
public class MtdSysConfigController {

    @Autowired
    private MtdSysConfigService mtdSysConfigService;

    /**
     * 保存系统公共配置表。
     *
     * @param mtdSysConfig 系统公共配置表
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    public Result<?> save(@RequestBody MtdSysConfig mtdSysConfig) {
        return Result.success(mtdSysConfigService.save(mtdSysConfig));
    }

    /**
     * 根据主键删除系统公共配置表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public Result<?> remove(@PathVariable Long id) {
        return Result.success(mtdSysConfigService.removeById(id));
    }

    /**
     * 根据主键更新系统公共配置表。
     *
     * @param mtdSysConfig 系统公共配置表
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public Result<?> update(@RequestBody MtdSysConfig mtdSysConfig) {
        return Result.success(mtdSysConfigService.updateById(mtdSysConfig));
    }

    /**
     * 查询所有系统公共配置表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public Result<List<MtdSysConfig>> list() {
        return Result.success(mtdSysConfigService.list());
    }

    /**
     * 根据主键获取系统公共配置表。
     *
     * @param id 系统公共配置表主键
     * @return 系统公共配置表详情
     */
    @GetMapping("getInfo/{id}")
    public Result<MtdSysConfig> getInfo(@PathVariable Long id) {
        return Result.success(mtdSysConfigService.getById(id));
    }

    /**
     * 分页查询系统公共配置表。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @PostMapping("page")
    public Result<Page<MtdSysConfig>> page(@RequestBody Page<MtdSysConfig> page) {
        return Result.success(mtdSysConfigService.page(page));
    }

}
