package com.mtd.tt.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.request.PageRequest;
import com.mtd.common.core.response.PageResponse;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.mapper.BlockMapper;
import com.mtd.tt.mapper.FollowMapper;
import com.mtd.tt.mapper.PostMapper;
import com.mtd.tt.mapper.UserMapper;
import com.mtd.tt.model.entity.BlockEntity;
import com.mtd.tt.model.entity.FollowEntity;
import com.mtd.tt.model.entity.UserEntity;
import com.mtd.tt.model.req.UpdateProfileReq;
import com.mtd.tt.model.resp.UserProfileResp;
import com.mtd.tt.enums.UserStatusEnum;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务：资料 / 关注 / 粉丝 / 拉黑 / 搜索
 */
@Slf4j
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FollowMapper followMapper;
    @Autowired
    private BlockMapper blockMapper;
    @Autowired
    private PostMapper postMapper;
    @Autowired
    private TimelineService timelineService;

    /**
     * 获取用户资料（含关系与统计）
     *
     * @param targetUserId  目标用户
     * @param currentUserId 当前登录用户（可为 null）
     */
    public UserProfileResp getProfile(Long targetUserId, Long currentUserId) {
        UserEntity user = getActiveUser(targetUserId);
        return buildProfile(user, currentUserId, true);
    }

    /**
     * 修改个人资料
     */
    public UserProfileResp updateProfile(Long currentUserId, UpdateProfileReq req) {
        requireLogin(currentUserId);
        UserEntity user = getActiveUser(currentUserId);
        if (StrUtil.isNotBlank(req.getNickname())) {
            user.setNickname(req.getNickname());
        }
        if (req.getAvatar() != null) {
            user.setAvatar(req.getAvatar());
        }
        if (req.getBanner() != null) {
            user.setBanner(req.getBanner());
        }
        if (req.getBio() != null) {
            user.setBio(req.getBio());
        }
        if (req.getUserPrivate() != null) {
            user.setUserPrivate(req.getUserPrivate());
        }
        userMapper.update(user);
        return buildProfile(user, currentUserId, true);
    }

    /**
     * 关注用户
     */
    public void follow(Long currentUserId, Long followeeId) {
        requireLogin(currentUserId);
        checkNotSelf(currentUserId, followeeId);
        getActiveUser(followeeId);
        if (isBlocked(followeeId, currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "你已被对方拉黑，无法关注");
        }
        if (isFollowing(currentUserId, followeeId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "已关注该用户");
        }
        FollowEntity follow = new FollowEntity();
        follow.setFollowerId(currentUserId);
        follow.setFolloweeId(followeeId);
        followMapper.insertSelective(follow);
        // 关注后：把被关注人最近 N 条推文回填到自己的时间线
        timelineService.fillOnFollow(currentUserId, followeeId);
    }

    /**
     * 取消关注
     */
    public void unfollow(Long currentUserId, Long followeeId) {
        requireLogin(currentUserId);
        FollowEntity follow = followMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq(FollowEntity::getFollowerId, currentUserId)
                        .eq(FollowEntity::getFolloweeId, followeeId));
        if (follow == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "未关注该用户");
        }
        followMapper.deleteById(follow.getId());
        // 取关后：把自己时间线里该人的推文移除
        timelineService.clearOnUnfollow(currentUserId, followeeId);
    }

    /**
     * 某用户关注的用户列表
     */
    public PageResponse<UserProfileResp> followingList(Long targetUserId, PageRequest page, Long currentUserId) {
        getActiveUser(targetUserId);
        Page<FollowEntity> p = followMapper.paginate(page.getPageNum(), page.getPageSize(),
                QueryWrapper.create()
                        .eq(FollowEntity::getFollowerId, targetUserId)
                        .orderBy(FollowEntity::getCreatedAt, false));
        List<Long> ids = p.getRecords().stream().map(FollowEntity::getFolloweeId).collect(Collectors.toList());
        List<UserProfileResp> list = listProfiles(ids, currentUserId);
        return PageResponse.of((int) p.getPageNumber(), (int) p.getPageSize(), p.getTotalRow(), list);
    }

    /**
     * 某用户的粉丝列表
     */
    public PageResponse<UserProfileResp> followerList(Long targetUserId, PageRequest page, Long currentUserId) {
        getActiveUser(targetUserId);
        Page<FollowEntity> p = followMapper.paginate(page.getPageNum(), page.getPageSize(),
                QueryWrapper.create()
                        .eq(FollowEntity::getFolloweeId, targetUserId)
                        .orderBy(FollowEntity::getCreatedAt, false));
        List<Long> ids = p.getRecords().stream().map(FollowEntity::getFollowerId).collect(Collectors.toList());
        List<UserProfileResp> list = listProfiles(ids, currentUserId);
        return PageResponse.of((int) p.getPageNumber(), (int) p.getPageSize(), p.getTotalRow(), list);
    }

    /**
     * 拉黑用户
     */
    public void block(Long currentUserId, Long blockedUserId) {
        requireLogin(currentUserId);
        checkNotSelf(currentUserId, blockedUserId);
        getActiveUser(blockedUserId);
        BlockEntity exist = blockMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq(BlockEntity::getUserId, currentUserId)
                        .eq(BlockEntity::getBlockedUserId, blockedUserId));
        if (exist != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "已拉黑该用户");
        }
        BlockEntity block = new BlockEntity();
        block.setUserId(currentUserId);
        block.setBlockedUserId(blockedUserId);
        blockMapper.insertSelective(block);
        // 拉黑后自动解除关注关系
        unfollowQuiet(currentUserId, blockedUserId);
        unfollowQuiet(blockedUserId, currentUserId);
    }

    /**
     * 取消拉黑
     */
    public void unblock(Long currentUserId, Long blockedUserId) {
        requireLogin(currentUserId);
        BlockEntity exist = blockMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq(BlockEntity::getUserId, currentUserId)
                        .eq(BlockEntity::getBlockedUserId, blockedUserId));
        if (exist == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "未拉黑该用户");
        }
        blockMapper.deleteById(exist.getId());
    }

    /**
     * 搜索用户（用户名/昵称模糊匹配）
     */
    public PageResponse<UserProfileResp> search(String keyword, PageRequest page, Long currentUserId) {
        if (StrUtil.isBlank(keyword)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "搜索关键词不能为空");
        }
        // 精确 username 命中者强制排在结果最前，便于 @补全/@解析按用户名精确定位（如 @3 → username='3' 的用户即使 id 靠后也排最前）
        List<UserEntity> exact = userMapper.selectListByQuery(QueryWrapper.create()
                .eq(UserEntity::getStatus, UserStatusEnum.ACTIVE.getCode())
                .eq(UserEntity::getUsername, keyword));

        QueryWrapper fw = QueryWrapper.create()
                .eq(UserEntity::getStatus, UserStatusEnum.ACTIVE.getCode())
                .and(w -> {
                    w.where(UserEntity::getUsername).like(keyword)
                            .or(UserEntity::getNickname).like(keyword);
                    // 纯数字关键词也按 userId 匹配，便于 @数字 方式直接引用某个用户（如 @3 命中 userId=3）
                    if (StrUtil.isNumeric(keyword)) {
                        w.or(UserEntity::getId).eq(Long.parseLong(keyword));
                    }
                })
                .orderBy(UserEntity::getId, true);
        List<UserEntity> fuzzy = userMapper.selectListByQuery(fw);

        // 合并去重（按 id），精确 username 排最前，其余保持 id 升序
        LinkedHashMap<Long, UserEntity> merged = new LinkedHashMap<>();
        exact.forEach(e -> merged.put(e.getId(), e));
        fuzzy.forEach(e -> merged.putIfAbsent(e.getId(), e));
        List<UserEntity> all = new ArrayList<>(merged.values());

        // 手动分页
        int fromIndex = (page.getPageNum() - 1) * page.getPageSize();
        List<UserEntity> pageRecords = all.size() > fromIndex
                ? all.subList(fromIndex, Math.min(all.size(), fromIndex + page.getPageSize()))
                : List.of();
        List<Long> ids = pageRecords.stream().map(UserEntity::getId).collect(Collectors.toList());
        List<UserProfileResp> list = listProfiles(ids, currentUserId);
        return PageResponse.of(page.getPageNum(), page.getPageSize(), (long) all.size(), list);
    }

    // ===================== 内部工具方法 =====================

    private UserEntity getActiveUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户ID不能为空");
        }
        UserEntity user = userMapper.selectOneById(userId);
        if (user == null || user.getDeletedAt() != null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        return user;
    }

    /**
     * 批量构建用户资料（保持传入顺序）
     */
    private List<UserProfileResp> listProfiles(List<Long> ids, Long currentUserId) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        return ids.stream()
                .map(id -> {
                    UserEntity u = userMapper.selectOneById(id);
                    return u == null ? null : buildProfile(u, currentUserId, false);
                })
                .filter(ObjectUtil::isNotNull)
                .collect(Collectors.toList());
    }

    /**
     * 构建用户资料 VO
     */
    public UserProfileResp buildProfile(UserEntity user, Long currentUserId, boolean needCounts) {
        UserProfileResp resp = new UserProfileResp();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setEmail(user.getEmail());
        resp.setAvatar(user.getAvatar());
        resp.setBanner(user.getBanner());
        resp.setBio(user.getBio());
        resp.setStatus(user.getStatus());
        resp.setUserPrivate(user.getUserPrivate());
        resp.setCreatedAt(user.getCreatedAt());

        boolean self = currentUserId != null && currentUserId.equals(user.getId());
        resp.setIsFollowed(!self && isFollowing(currentUserId, user.getId()));
        resp.setIsBlocked(!self && isBlocked(currentUserId, user.getId()));

        if (needCounts) {
            resp.setFollowingCount(countFollowing(user.getId()));
            resp.setFollowerCount(countFollowers(user.getId()));
            resp.setPostCount(countPosts(user.getId()));
        }
        return resp;
    }

    private long countFollowing(Long userId) {
        return followMapper.selectCountByQuery(
                QueryWrapper.create().eq(FollowEntity::getFollowerId, userId));
    }

    private long countFollowers(Long userId) {
        return followMapper.selectCountByQuery(
                QueryWrapper.create().eq(FollowEntity::getFolloweeId, userId));
    }

    private long countPosts(Long userId) {
        return postMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq(com.mtd.tt.model.entity.PostEntity::getUserId, userId)
                        .isNull(com.mtd.tt.model.entity.PostEntity::getDeletedAt));
    }

    public boolean isFollowing(Long followerId, Long followeeId) {
        if (followerId == null) {
            return false;
        }
        Long cnt = followMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq(FollowEntity::getFollowerId, followerId)
                        .eq(FollowEntity::getFolloweeId, followeeId));
        return cnt != null && cnt > 0;
    }

    /**
     * 某用户关注的用户 ID 列表（含自己，供首页时间线使用）
     */
    public List<Long> followingUserIds(Long userId) {
        List<FollowEntity> follows = followMapper.selectListByQuery(
                QueryWrapper.create().eq(FollowEntity::getFollowerId, userId));
        List<Long> ids = follows.stream().map(FollowEntity::getFolloweeId).collect(Collectors.toList());
        if (!ids.contains(userId)) {
            ids.add(userId);
        }
        return ids;
    }

    public boolean isBlocked(Long userId, Long blockedUserId) {
        if (userId == null) {
            return false;
        }
        Long cnt = blockMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq(BlockEntity::getUserId, userId)
                        .eq(BlockEntity::getBlockedUserId, blockedUserId));
        return cnt != null && cnt > 0;
    }

    private void requireLogin(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "请先登录");
        }
    }

    private void checkNotSelf(Long currentUserId, Long targetId) {
        if (currentUserId.equals(targetId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不能对自己执行该操作");
        }
    }

    private void unfollowQuiet(Long followerId, Long followeeId) {
        FollowEntity exist = followMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq(FollowEntity::getFollowerId, followerId)
                        .eq(FollowEntity::getFolloweeId, followeeId));
        if (exist != null) {
            followMapper.deleteById(exist.getId());
        }
    }
}
