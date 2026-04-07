package com.mtd.mfl.controller;

import com.mybatisflex.core.paginate.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import com.mtd.mfl.entity.SysUser;
import com.mtd.mfl.service.SysUserService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

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
    public boolean save(@RequestBody SysUser sysUser) {
        return sysUserService.save(sysUser);
    }

    /**
     * 根据主键删除用户表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return sysUserService.removeById(id);
    }

    /**
     * 根据主键更新用户表。
     *
     * @param sysUser 用户表
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody SysUser sysUser) {
        return sysUserService.updateById(sysUser);
    }

    /**
     * 查询所有用户表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<SysUser> list() {
        return sysUserService.list();
    }

    /**
     * 根据主键获取用户表。
     *
     * @param id 用户表主键
     * @return 用户表详情
     */
    @GetMapping("getInfo/{id}")
    public SysUser getInfo(@PathVariable Long id) {
        return sysUserService.getById(id);
    }

    /**
     * 分页查询用户表。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @PostMapping("page")
    public Page<SysUser> page(@RequestBody Page<SysUser> page) {
        return sysUserService.page(page);
    }

}
