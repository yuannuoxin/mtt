package com.mtd.demo.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.mtd.common.core.constant.CommonConstants;
import com.mtd.common.core.request.IdRequest;
import com.mtd.common.core.response.PageResponse;
import com.mtd.common.core.result.Result;
import com.mtd.common.core.result.ResultCode;
import com.mtd.common.mybatis.base.BaseEntity;
import com.mtd.demo.entity.User;
import com.mtd.demo.request.MockUserBatchRequest;
import com.mtd.demo.request.UserListRequest;
import com.mtd.demo.request.UserPageRequest;
import com.mtd.demo.request.UserSaveRequest;
import com.mtd.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户控制器
 */
@Slf4j
@Tag(name = "用户管理", description = "用户相关接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 构建排序条件
     *
     * @param wrapper   查询包装器
     * @param sortField 排序字段
     * @param sortOrder 排序方向（asc/desc）
     */
    private void buildSortCondition(LambdaQueryWrapper<User> wrapper, String sortField, String sortOrder) {
        // 排序处理（根据字段名动态排序）
        if (StringUtils.isNotBlank(sortField)) {
            boolean isAsc = CommonConstants.SortOrder.ASC.equalsIgnoreCase(sortOrder);

            switch (sortField) {
                case BaseEntity.Fields.id:
                    wrapper.orderBy(true, isAsc, User::getId);
                    break;
                case User.Fields.username:
                    wrapper.orderBy(true, isAsc, User::getUsername);
                    break;
                case User.Fields.nickname:
                    wrapper.orderBy(true, isAsc, User::getNickname);
                    break;
                case User.Fields.email:
                    wrapper.orderBy(true, isAsc, User::getEmail);
                    break;
                case User.Fields.phone:
                    wrapper.orderBy(true, isAsc, User::getPhone);
                    break;
                case User.Fields.status:
                    wrapper.orderBy(true, isAsc, User::getStatus);
                    break;
                case BaseEntity.Fields.createTime:
                    wrapper.orderBy(true, isAsc, User::getCreateTime);
                    break;
                case BaseEntity.Fields.updateTime:
                    wrapper.orderBy(true, isAsc, User::getUpdateTime);
                    break;
                default:
                    // 未知字段不排序
                    break;
            }
        }
        wrapper.orderByDesc(BaseEntity::getId);
    }

    @Operation(summary = "根据 ID 查询用户", description = "通过用户 ID 获取用户详细信息")
    @PostMapping("/get")
    public Result<User> get(@RequestBody IdRequest request) {
        User user = userService.getById(request.getId());
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(user);
    }

    @Operation(summary = "查询用户列表", description = "查询所有符合条件的用户（不分页，返回全部数据）")
    @PostMapping("/list/all")
    public Result<java.util.List<User>> listAll(@RequestBody(required = false) UserListRequest request) {
        // 如果请求参数为空，使用默认参数
        if (request == null) {
            request = new UserListRequest();
        }

        // 构建查询条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(request.getUsername()), User::getUsername, request.getUsername())
                .like(StringUtils.isNotBlank(request.getPhone()), User::getPhone, request.getPhone())
                .eq(request.getStatus() != null, User::getStatus, request.getStatus());

        // 添加排序条件
        buildSortCondition(wrapper, request.getSortField(), request.getSortOrder());

        // 执行查询（返回所有数据）
        java.util.List<User> userList = userService.list(wrapper);

        return Result.success(userList);
    }

    @Operation(summary = "查询用户分页", description = "分页查询用户信息，支持用户名、手机号、状态筛选和排序")
    @PostMapping("/list/page")
    public Result<PageResponse<User>> listPage(@RequestBody(required = false) UserPageRequest request) {
        log.info("分页查询用户信息：{}", request);
        // 如果请求参数为空，使用默认参数
        if (request == null) {
            request = new UserPageRequest();
        }

        // 构建分页对象
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        request.getPageNum(),
                        request.getPageSize()
                );

        // 构建查询条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(request.getUsername()), User::getUsername, request.getUsername())
                .like(StringUtils.isNotBlank(request.getPhone()), User::getPhone, request.getPhone())
                .eq(request.getStatus() != null, User::getStatus, request.getStatus());

        // 添加排序条件
        buildSortCondition(wrapper, request.getSortField(), request.getSortOrder());

        // 执行分页查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> resultPage = userService.page(page, wrapper);

        return Result.success(PageResponse.of(
                (int) resultPage.getCurrent(),
                (int) resultPage.getSize(),
                resultPage.getTotal(),
                resultPage.getRecords()
        ));
    }

    @Operation(summary = "创建用户", description = "新增用户信息")
    @PostMapping("/save")
    public Result<Boolean> save(@RequestBody UserSaveRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus());
        boolean saved = userService.save(user);
        return Result.success(saved);
    }

    @Operation(summary = "更新用户", description = "根据 ID 更新用户信息")
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody UserSaveRequest request) {
        User user = new User();
        user.setId(request.getId());
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus());
        boolean updated = userService.updateById(user);
        return Result.success(updated);
    }

    @Operation(summary = "删除用户", description = "根据 ID 删除用户（逻辑删除）")
    @PostMapping("/delete")
    public Result<Boolean> delete(@RequestBody IdRequest request) {
        boolean removed = userService.removeById(request.getId());
        return Result.success(removed);
    }

    @Operation(summary = "批量创建模拟用户", description = "批量生成指定数量的测试用户数据")
    @PostMapping("/batch/mock")
    public Result<Integer> batchCreateMockUsers(@RequestBody(required = false) MockUserBatchRequest request) {
        // 如果请求参数为空，使用默认参数
        if (request == null) {
            request = new MockUserBatchRequest();
        }

        Integer count = request.getCount();
        List<User> userList = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            User user = new User();
            // 生成随机用户名
            String username = "user_" + RandomUtil.randomString(6);
            user.setUsername(username);
            // 默认密码 123456，使用 MD5 加密
            user.setPassword(SecureUtil.md5("123456"));
            // 生成随机昵称
            user.setNickname("用户" + RandomUtil.randomInt(1000, 9999));
            // 生成随机邮箱
            user.setEmail(String.format("%s@%s.com",
                    RandomUtil.randomString(8),
                    RandomUtil.randomEle(new String[]{"gmail", "qq", "163", "hotmail"})));
            // 生成随机手机号
            user.setPhone("1" + String.valueOf(RandomUtil.randomLong(1000000000L, 1999999999L)));
            // 随机状态
            user.setStatus(RandomUtil.randomEle(new Integer[]{0, 1}));

            // createTime、updateTime、createBy、updateBy 由 MyBatis-Plus 自动填充

            userList.add(user);
        }

        // 批量保存 - 使用事务确保数据一致性
        boolean saved = userService.saveBatch(userList);

        return Result.success(saved ? count : 0);
    }
}
