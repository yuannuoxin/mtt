package com.mtd.tt.service;

import cn.hutool.core.collection.CollUtil;
import com.mtd.tt.enums.PostTypeEnum;
import com.mtd.tt.enums.UserTimelineItemTypeEnum;
import com.mtd.tt.mapper.FollowMapper;
import com.mtd.tt.mapper.PostMapper;
import com.mtd.tt.mapper.UserTimelineMapper;
import com.mtd.tt.model.entity.FollowEntity;
import com.mtd.tt.model.entity.PostEntity;
import com.mtd.tt.model.entity.UserTimelineEntity;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 首页关注流时间线维护。
 * <p>user_timeline 表只为首页关注列表设计，不维护全量：
 * <ul>
 *   <li>关注某人：把他最近 N 条推文回填到自己的时间线</li>
 *   <li>发新推文：推送到我的全部粉丝时间线（fanout）</li>
 *   <li>取关：把自己时间线里该人的推文移除</li>
 *   <li>删帖：从所有时间线里移除该帖</li>
 * </ul>
 * 想看某人的全量推文，用户到其主页查 userPosts 即可，与时间线无关。
 */
@Slf4j
@Service
public class TimelineService {

    /** 关注时回填被关注人最近推文的条数 */
    private static final int BACKFILL_SIZE = 50;

    @Autowired
    private UserTimelineMapper timelineMapper;
    @Autowired
    private FollowMapper followMapper;
    @Autowired
    private PostMapper postMapper;

    /**
     * 发帖成功后推送（fanout）到作者全部粉丝的时间线
     */
    public void pushOnPostCreated(PostEntity post) {
        if (post == null || post.getId() == null) {
            return;
        }
        // 首页时间线只放独立推文，回复不 fanout，避免评论作为一条新动态插到前面
        if (PostTypeEnum.REPLY.getCode().equals(post.getType())) {
            return;
        }
        Long authorId = post.getUserId();
        // 关注流与推特一致：只推送给粉丝，不含作者自己（自己的推文在个人主页查看）
        List<Long> followerIds = followerUserIds(authorId);
        if (CollUtil.isEmpty(followerIds)) {
            return;
        }
        for (Long followerId : followerIds) {
            UserTimelineEntity item = new UserTimelineEntity();
            item.setUserId(followerId);
            item.setItemType(UserTimelineItemTypeEnum.POST.getCode());
            item.setItemId(post.getId());
            item.setItemUserId(authorId);
            item.setValid(Boolean.TRUE);
            // 用插入时间：insertSelective 不会把 DB 默认值回填到实体，避免 created_at 为空
            item.setCreatedAt(OffsetDateTime.now());
            try {
                timelineMapper.insertSelective(item);
            } catch (Exception e) {
                // 已有同 (user_id, item_id) 记录则忽略，避免唯一键冲突
                log.warn("时间线推送忽略重复: user={}, post={}", followerId, post.getId());
            }
        }
    }

    /**
     * 关注某人时，把他最近 N 条推文回填到自己的时间线（先清空该人的旧记录再回填最新）
     */
    public void fillOnFollow(Long followerId, Long followeeId) {
        // 移除该人的旧时间线条目，避免回填与历史推送重复
        timelineMapper.deleteByQuery(QueryWrapper.create()
                .eq(UserTimelineEntity::getUserId, followerId)
                .eq(UserTimelineEntity::getItemUserId, followeeId));
        List<PostEntity> recent = postMapper.selectListByQuery(QueryWrapper.create()
                .eq(PostEntity::getUserId, followeeId)
                .isNull(PostEntity::getDeletedAt)
                // 回填只放独立推文，回复留在原帖详情里
                .ne(PostEntity::getType, PostTypeEnum.REPLY.getCode())
                .orderBy(PostEntity::getId, false)
                .limit(BACKFILL_SIZE));
        if (CollUtil.isEmpty(recent)) {
            return;
        }
        for (PostEntity post : recent) {
            UserTimelineEntity item = new UserTimelineEntity();
            item.setUserId(followerId);
            item.setItemType(UserTimelineItemTypeEnum.POST.getCode());
            item.setItemId(post.getId());
            item.setItemUserId(followeeId);
            item.setValid(Boolean.TRUE);
            item.setCreatedAt(OffsetDateTime.now());
            try {
                timelineMapper.insertSelective(item);
            } catch (Exception e) {
                log.warn("时间线回填忽略重复: user={}, post={}", followerId, post.getId());
            }
        }
    }

    /**
     * 取关某人时，把自己时间线里该人的推文全部移除
     */
    public void clearOnUnfollow(Long followerId, Long followeeId) {
        timelineMapper.deleteByQuery(QueryWrapper.create()
                .eq(UserTimelineEntity::getUserId, followerId)
                .eq(UserTimelineEntity::getItemUserId, followeeId));
    }

    /**
     * 删除帖子时，从所有时间线里移除该帖
     */
    public void clearOnPostDeleted(Long postId) {
        timelineMapper.deleteByQuery(QueryWrapper.create()
                .eq(UserTimelineEntity::getItemId, postId));
    }

    private List<Long> followerUserIds(Long userId) {
        List<FollowEntity> follows = followMapper.selectListByQuery(
                QueryWrapper.create().eq(FollowEntity::getFolloweeId, userId));
        return follows.stream().map(FollowEntity::getFollowerId).collect(Collectors.toList());
    }
}
