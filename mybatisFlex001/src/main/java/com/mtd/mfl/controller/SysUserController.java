package com.mtd.mfl.controller;

import com.mtd.common.core.result.Result;
import com.mtd.mfl.entity.SysUser;
import com.mtd.mfl.service.SysUserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户表 控制层。
 *
 * @author user
 * @since 2026-04-07
 */
@RestController
@RequestMapping("/sysUser")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 保存用户表。
     *
     * @param sysUser 用户表
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    public Result<?> save(@RequestBody SysUser sysUser) {
        return Result.success(sysUserService.save(sysUser));
    }

    /**
     * 根据主键删除用户表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public Result<?> remove(@PathVariable Long id) {
        return Result.success(sysUserService.removeById(id));
    }

    /**
     * 根据主键更新用户表。
     *
     * @param sysUser 用户表
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public Result<?> update(@RequestBody SysUser sysUser) {
        return Result.success(sysUserService.updateById(sysUser));
    }

    /**
     * 查询所有用户表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public Result<?> list() {
        return Result.success(sysUserService.list());
    }

    /**
     * 根据主键获取用户表。
     *
     * @param id 用户表主键
     * @return 用户表详情
     */
    @GetMapping("getInfo/{id}")
    public Result<?> getInfo(@PathVariable Long id) {
        return Result.success(sysUserService.getById(id));
    }

    /**
     * 分页查询用户表。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @PostMapping("page")
    public Result<?> page(@RequestBody Page<SysUser> page) {
        return Result.success(sysUserService.page(page));
    }

    @PostMapping("/selectListByMap")
    public Result<List<SysUser>> selectListByMap(@RequestBody Map<String, Object> conditions) {
        // ✅ Flex 自动解析：key里的 _like / _gt / _gte
        QueryWrapper wrapper = QueryWrapper.create()
                .where(conditions); // 🔥 核心方法
        return Result.success(sysUserService.list(wrapper));
    }

}
