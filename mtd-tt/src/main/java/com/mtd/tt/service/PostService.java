package com.mtd.tt.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.request.PageRequest;
import com.mtd.common.core.response.PageResponse;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.mapper.BookmarkMapper;
import com.mtd.tt.mapper.PostLikeMapper;
import com.mtd.tt.mapper.PostLinkCardMapper;
import com.mtd.tt.mapper.PostMapper;
import com.mtd.tt.mapper.PostMediaMapper;
import com.mtd.tt.mapper.PostMentionMapper;
import com.mtd.tt.mapper.UserMapper;
import com.mtd.tt.model.entity.BookmarkEntity;
import com.mtd.tt.model.entity.PostEntity;
import com.mtd.tt.model.entity.PostLikeEntity;
import com.mtd.tt.model.entity.PostLinkCardEntity;
import com.mtd.tt.model.entity.PostMediaEntity;
import com.mtd.tt.model.entity.PostMentionEntity;
import com.mtd.tt.model.entity.UserEntity;
import com.mtd.tt.model.req.CreatePostLinkCardReq;
import com.mtd.tt.model.req.CreatePostMediaReq;
import com.mtd.tt.model.req.CreatePostMentionReq;
import com.mtd.tt.model.req.CreatePostReq;
import com.mtd.tt.model.resp.PostLinkCardResp;
import com.mtd.tt.model.resp.PostMediaResp;
import com.mtd.tt.model.resp.PostMentionResp;
import com.mtd.tt.model.resp.PostResp;
import com.mtd.tt.model.resp.UserProfileResp;
import com.mtd.tt.enums.CardTypeEnum;
import com.mtd.tt.enums.MediaTypeEnum;
import com.mtd.tt.enums.MentionTypeEnum;
import com.mtd.tt.enums.NotificationTypeEnum;
import com.mtd.tt.enums.PostTypeEnum;
import com.mtd.tt.enums.PostVisibilityEnum;
import com.mtd.tt.enums.ReplyPermissionEnum;
import com.mtd.tt.util.RedisPostCounter;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 帖子服务：发帖 / 详情 / 时间线 / 搜索 / 点赞 / 收藏 / 回复 / 转发
 */
@Slf4j
@Service
public class PostService {

    /** 单帖图片上限 */
    private static final int MAX_IMAGES = 4;

    @Autowired
    private PostMapper postMapper;
    @Autowired
    private PostMediaMapper postMediaMapper;
    @Autowired
    private PostMentionMapper postMentionMapper;
    @Autowired
    private PostLinkCardMapper postLinkCardMapper;
    @Autowired
    private PostLikeMapper postLikeMapper;
    @Autowired
    private BookmarkMapper bookmarkMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private RedisPostCounter redisCounter;

    /**
     * 创建帖子（original / reply / quote / repost）
     */
    @Transactional(rollbackFor = Exception.class)
    public PostResp createPost(Long currentUserId, CreatePostReq req) {
        requireLogin(currentUserId);
        String type = StrUtil.isBlank(req.getType()) ? PostTypeEnum.ORIGINAL.getCode() : req.getType();
        PostTypeEnum typeEnum = PostTypeEnum.of(type);
        if (typeEnum == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不支持的帖子类型: " + type);
        }

        PostEntity referenced = null;
        if (typeEnum != PostTypeEnum.ORIGINAL) {
            if (req.getReferencedPostId() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "该类型帖子必须指定被引用帖子");
            }
            referenced = getActivePost(req.getReferencedPostId());
        }

        // 组装帖子
        PostEntity post = new PostEntity();
        post.setUserId(currentUserId);
        post.setType(type);
        post.setContent(req.getContent());
        post.setReferencedPostId(req.getReferencedPostId());
        // reply：root_post_id 指向引用帖的根
        if (typeEnum == PostTypeEnum.REPLY) {
            post.setRootPostId(resolveRootId(referenced));
        }
        post.setVisibility(StrUtil.blankToDefault(req.getVisibility(), PostVisibilityEnum.PUBLIC.getCode()));
        post.setArticleFlag(0);
        post.setReplyPermission(StrUtil.blankToDefault(req.getReplyPermission(), ReplyPermissionEnum.ANYONE.getCode()));

        // 媒体标记
        boolean hasMedia = CollUtil.isNotEmpty(req.getMedias());
        boolean hasImage = hasMedia && req.getMedias().stream()
                .anyMatch(m -> MediaTypeEnum.IMAGE.getCode().equals(m.getMediaType()));
        boolean hasVideo = hasMedia && req.getMedias().stream()
                .anyMatch(m -> MediaTypeEnum.VIDEO.getCode().equals(m.getMediaType()));
        post.setHasMedia(hasMedia);
        post.setHasImage(hasImage);
        post.setHasVideo(hasVideo);

        post.setLikeCount(0L);
        post.setReplyCount(0L);
        post.setRepostCount(0L);
        post.setViewCount(0L);
        post.setBookmarkCount(0L);
        post.setHotScore(0L);
        postMapper.insertSelective(post);
        Long postId = post.getId();

        // 媒体
        if (hasMedia) {
            // 校验类型与数量：图片最多4张；视频/音频合计最多1个；图片与音视频互斥
            long imgCount = req.getMedias().stream()
                    .filter(m -> MediaTypeEnum.IMAGE.getCode().equals(m.getMediaType())).count();
            long nonImgCount = req.getMedias().stream()
                    .filter(m -> MediaTypeEnum.VIDEO.getCode().equals(m.getMediaType())
                            || MediaTypeEnum.AUDIO.getCode().equals(m.getMediaType())).count();
            if (imgCount > MAX_IMAGES) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "图片最多上传 " + MAX_IMAGES + " 张");
            }
            if (nonImgCount > 1) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "视频/音频最多上传 1 个");
            }
            if (imgCount > 0 && nonImgCount > 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "图片与音视频不能同时上传");
            }
            for (CreatePostMediaReq item : req.getMedias()) {
                String mediaType = item.getMediaType();
                if (!MediaTypeEnum.IMAGE.getCode().equals(mediaType)
                        && !MediaTypeEnum.VIDEO.getCode().equals(mediaType)
                        && !MediaTypeEnum.AUDIO.getCode().equals(mediaType)) {
                    throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不支持的媒体类型: " + mediaType);
                }
                if (MediaTypeEnum.IMAGE.getCode().equals(mediaType) && StrUtil.isBlank(item.getOriginalUrl())) {
                    throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "图片缺少 originalUrl");
                }
                if (MediaTypeEnum.VIDEO.getCode().equals(mediaType)) {
                    if (StrUtil.isBlank(item.getPlayUrl())) {
                        throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "视频缺少 playUrl");
                    }
                    if (StrUtil.isBlank(item.getOriginalUrl())) {
                        throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "视频缺少封面 originalUrl");
                    }
                }
                if (MediaTypeEnum.AUDIO.getCode().equals(mediaType) && StrUtil.isBlank(item.getPlayUrl())) {
                    throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "音频缺少 playUrl");
                }
                PostMediaEntity media = new PostMediaEntity();
                media.setPostId(postId);
                media.setMediaType(item.getMediaType());
                // audio 无封面，置空串满足 DB NOT NULL；渲染时按 mediaType 走 playUrl
                media.setOriginalUrl(StrUtil.nullToEmpty(item.getOriginalUrl()));
                media.setThumbnailUrl(StrUtil.nullToEmpty(item.getThumbnailUrl()));
                media.setPlayUrl(item.getPlayUrl());
                media.setWidth(item.getWidth());
                media.setHeight(item.getHeight());
                media.setDuration(item.getDuration());
                postMediaMapper.insertSelective(media);
            }
        }

        // @提及
        if (CollUtil.isNotEmpty(req.getMentions())) {
            for (CreatePostMentionReq item : req.getMentions()) {
                PostMentionEntity mention = new PostMentionEntity();
                mention.setPostId(postId);
                mention.setMentionType(item.getMentionType());
                mention.setTargetId(item.getTargetId());
                mention.setStartIdx(item.getStartIdx());
                mention.setEndIdx(item.getEndIdx());
                postMentionMapper.insertSelective(mention);
            }
        }

        // 链接卡片
        if (req.getLinkCard() != null) {
            PostLinkCardEntity card = new PostLinkCardEntity();
            card.setPostId(postId);
            card.setCardType(StrUtil.blankToDefault(req.getLinkCard().getCardType(), CardTypeEnum.LINK.getCode()));
            card.setTargetUrl(req.getLinkCard().getTargetUrl());
            card.setVideoProvider(req.getLinkCard().getVideoProvider());
            card.setVideoId(req.getLinkCard().getVideoId());
            card.setCoverMediaUrl(req.getLinkCard().getCoverMediaUrl());
            postLinkCardMapper.insertSelective(card);
        }

        // 更新被引用帖计数 + 生成通知（计数走 Redis，不再每次 UPDATE posts 表）
        if (referenced != null) {
            switch (typeEnum) {
                case REPLY -> {
                    redisCounter.incr(referenced.getId(), RedisPostCounter.REPLY);
                    notify(referenced.getUserId(), currentUserId, NotificationTypeEnum.COMMENT.getCode(), referenced.getId());
                }
                case QUOTE, REPOST -> {
                    if (typeEnum == PostTypeEnum.REPOST) {
                        redisCounter.addUser(referenced.getId(), RedisPostCounter.REPOST, currentUserId); // SADD + 计数 +1
                    } else {
                        redisCounter.incr(referenced.getId(), RedisPostCounter.REPOST); // quote 仅计数 +1
                    }
                    notify(referenced.getUserId(), currentUserId, NotificationTypeEnum.REPOST.getCode(), referenced.getId());
                }
                default -> {
                }
            }
        }

        // 被提及用户通知
        if (CollUtil.isNotEmpty(req.getMentions())) {
            for (CreatePostMentionReq item : req.getMentions()) {
                if (MentionTypeEnum.USER.getCode().equals(item.getMentionType())) {
                    notify(item.getTargetId(), currentUserId, NotificationTypeEnum.MENTION.getCode(), postId);
                }
            }
        }

        return buildPostResp(post, currentUserId);
    }

    /**
     * 删除帖子（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long currentUserId, Long postId) {
        requireLogin(currentUserId);
        PostEntity post = getActivePost(postId);
        if (!post.getUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能删除自己的帖子");
        }
        post.setDeletedAt(OffsetDateTime.now());
        postMapper.update(post);
    }

    /**
     * 帖子详情（浏览 +1）
     */
    public PostResp postDetail(Long postId, Long currentUserId) {
        PostEntity post = getActivePost(postId);
        checkViewPermission(post, currentUserId);
        redisCounter.incr(postId, RedisPostCounter.VIEW); // 浏览计数走 Redis
        return buildPostResp(post, currentUserId);
    }

    /**
     * 上报"已看"：前端在列表页点击图片查看原图、播放视频/音频时调用，
     * 与打开详情页一样累加浏览计数（游客也可调用，不计权限）。
     */
    public void recordView(Long postId) {
        getActivePost(postId);
        redisCounter.incr(postId, RedisPostCounter.VIEW);
    }

    /**
     * 某用户的帖子列表
     */
    public PageResponse<PostResp> userPosts(Long targetUserId, PageRequest page, String postType, Long currentUserId) {
        userService.getProfile(targetUserId, currentUserId);
        QueryWrapper wrapper = QueryWrapper.create()
                .eq(PostEntity::getUserId, targetUserId)
                .isNull(PostEntity::getDeletedAt);
        if (StrUtil.isNotBlank(postType)) {
            wrapper.eq(PostEntity::getType, postType);
        }
        wrapper.orderBy(PostEntity::getCreatedAt, false);
        return pagedPosts(wrapper, page, currentUserId);
    }

    /**
     * 首页关注时间线（关注的人 + 自己）
     */
    public PageResponse<PostResp> homeTimeline(Long currentUserId, PageRequest page) {
        if (currentUserId == null) {
            // 未登录：关注流为空（全量最新推文走 globalTimeline）
            return PageResponse.of(page.getPageNum(), page.getPageSize(), 0L, List.of());
        }
        List<Long> followingIds = userService.followingUserIds(currentUserId);
        followingIds.add(currentUserId);
        QueryWrapper wrapper = QueryWrapper.create()
                .in(PostEntity::getUserId, followingIds)
                .isNull(PostEntity::getDeletedAt)
                .orderBy(PostEntity::getCreatedAt, false);
        return pagedPosts(wrapper, page, currentUserId);
    }

    /**
     * 全量最新推文（不区分关注，类似推特 Latest）
     */
    public PageResponse<PostResp> globalTimeline(Long currentUserId, PageRequest page) {
        QueryWrapper wrapper = QueryWrapper.create()
                .isNull(PostEntity::getDeletedAt)
                .orderBy(PostEntity::getCreatedAt, false);
        return pagedPosts(wrapper, page, currentUserId);
    }

    /**
     * 某帖子的回复列表
     */
    public PageResponse<PostResp> replies(Long postId, Long currentUserId, PageRequest page) {
        getActivePost(postId);
        QueryWrapper wrapper = QueryWrapper.create()
                .eq(PostEntity::getReferencedPostId, postId)
                .eq(PostEntity::getType, PostTypeEnum.REPLY.getCode())
                .isNull(PostEntity::getDeletedAt)
                .orderBy(PostEntity::getCreatedAt, true);
        return pagedPosts(wrapper, page, currentUserId);
    }

    /**
     * 搜索帖子（正文模糊匹配）
     */
    public PageResponse<PostResp> search(String keyword, Long currentUserId, PageRequest page) {
        if (StrUtil.isBlank(keyword)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "搜索关键词不能为空");
        }
        QueryWrapper wrapper = QueryWrapper.create()
                .like(PostEntity::getContent, keyword)
                .isNull(PostEntity::getDeletedAt)
                .orderBy(PostEntity::getHotScore, false)
                .orderBy(PostEntity::getCreatedAt, false);
        return pagedPosts(wrapper, page, currentUserId);
    }

    /**
     * 点赞
     */
    @Transactional(rollbackFor = Exception.class)
    public void like(Long currentUserId, Long postId) {
        requireLogin(currentUserId);
        PostEntity post = getActivePost(postId);
        if (redisCounter.hasUser(postId, RedisPostCounter.LIKE, currentUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "已点赞");
        }
        PostLikeEntity like = new PostLikeEntity();
        like.setUserId(currentUserId);
        like.setPostId(postId);
        postLikeMapper.insertSelective(like); // DB 持久
        redisCounter.addUser(postId, RedisPostCounter.LIKE, currentUserId); // Redis 计数 + 关系
        notify(post.getUserId(), currentUserId, NotificationTypeEnum.LIKE.getCode(), postId);
    }

    /**
     * 取消点赞
     */
    @Transactional(rollbackFor = Exception.class)
    public void unlike(Long currentUserId, Long postId) {
        requireLogin(currentUserId);
        getActivePost(postId);
        PostLikeEntity exist = postLikeMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq(PostLikeEntity::getUserId, currentUserId)
                        .eq(PostLikeEntity::getPostId, postId));
        if (exist == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "未点赞");
        }
        postLikeMapper.deleteById(exist.getId());
        redisCounter.removeUser(postId, RedisPostCounter.LIKE, currentUserId);
    }

    /**
     * 收藏
     */
    @Transactional(rollbackFor = Exception.class)
    public void bookmark(Long currentUserId, Long postId) {
        requireLogin(currentUserId);
        getActivePost(postId);
        if (redisCounter.hasUser(postId, RedisPostCounter.BOOKMARK, currentUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "已收藏");
        }
        BookmarkEntity bookmark = new BookmarkEntity();
        bookmark.setUserId(currentUserId);
        bookmark.setPostId(postId);
        bookmarkMapper.insertSelective(bookmark); // DB 持久
        redisCounter.addUser(postId, RedisPostCounter.BOOKMARK, currentUserId);
    }

    /**
     * 取消收藏
     */
    @Transactional(rollbackFor = Exception.class)
    public void unbookmark(Long currentUserId, Long postId) {
        requireLogin(currentUserId);
        getActivePost(postId);
        BookmarkEntity exist = bookmarkMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq(BookmarkEntity::getUserId, currentUserId)
                        .eq(BookmarkEntity::getPostId, postId));
        if (exist == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "未收藏");
        }
        bookmarkMapper.deleteById(exist.getId());
        redisCounter.removeUser(postId, RedisPostCounter.BOOKMARK, currentUserId);
    }

    /**
     * 我的收藏列表
     */
    public PageResponse<PostResp> myBookmarks(Long currentUserId, PageRequest page) {
        requireLogin(currentUserId);
        Page<BookmarkEntity> p = bookmarkMapper.paginate(page.getPageNum(), page.getPageSize(),
                QueryWrapper.create()
                        .eq(BookmarkEntity::getUserId, currentUserId)
                        .orderBy(BookmarkEntity::getCreatedAt, false));
        List<Long> postIds = p.getRecords().stream().map(BookmarkEntity::getPostId).collect(Collectors.toList());
        List<PostResp> list = CollUtil.isEmpty(postIds) ? List.of() : postIds.stream()
                .map(id -> {
                    PostEntity post = postMapper.selectOneById(id);
                    if (post == null || post.getDeletedAt() != null) {
                        return null;
                    }
                    try {
                        checkViewPermission(post, currentUserId);
                    } catch (BusinessException e) {
                        return null;
                    }
                    return buildPostResp(post, currentUserId);
                })
                .filter(ObjectUtil::isNotNull)
                .collect(Collectors.toList());
        return PageResponse.of((int) p.getPageNumber(), (int) p.getPageSize(), p.getTotalRow(), list);
    }

    // ===================== 构建与内部工具 =====================

    private PageResponse<PostResp> pagedPosts(QueryWrapper wrapper, PageRequest page, Long currentUserId) {
        Page<PostEntity> p = postMapper.paginate(page.getPageNum(), page.getPageSize(), wrapper);
        List<PostResp> list = p.getRecords().stream()
                .map(post -> {
                    try {
                        checkViewPermission(post, currentUserId);
                    } catch (BusinessException e) {
                        return null;
                    }
                    return buildPostResp(post, currentUserId);
                })
                .filter(ObjectUtil::isNotNull)
                .collect(Collectors.toList());
        return PageResponse.of((int) p.getPageNumber(), (int) p.getPageSize(), p.getTotalRow(), list);
    }

    /**
     * 写操作前置校验：未登录抛出业务异常
     */
    private void requireLogin(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "请先登录");
        }
    }

    /**
     * 构建帖子 VO
     */
    private PostResp buildPostResp(PostEntity post, Long currentUserId) {
        return buildPostResp(post, currentUserId, 0);
    }

    private PostResp buildPostResp(PostEntity post, Long currentUserId, int depth) {
        PostResp resp = new PostResp();
        resp.setId(post.getId());
        resp.setUserId(post.getUserId());
        resp.setType(post.getType());
        resp.setContent(post.getContent());
        resp.setReferencedPostId(post.getReferencedPostId());
        resp.setRootPostId(post.getRootPostId());
        resp.setVisibility(post.getVisibility());
        resp.setArticleFlag(post.getArticleFlag());
        resp.setHasMedia(post.getHasMedia());
        resp.setHasImage(post.getHasImage());
        resp.setHasVideo(post.getHasVideo());
        resp.setReplyPermission(post.getReplyPermission());
        resp.setLikeCount(redisCounter.getCount(post.getId(), RedisPostCounter.LIKE, post.getLikeCount()));
        resp.setReplyCount(redisCounter.getCount(post.getId(), RedisPostCounter.REPLY, post.getReplyCount()));
        resp.setRepostCount(redisCounter.getCount(post.getId(), RedisPostCounter.REPOST, post.getRepostCount()));
        resp.setViewCount(redisCounter.getCount(post.getId(), RedisPostCounter.VIEW, post.getViewCount()));
        resp.setBookmarkCount(redisCounter.getCount(post.getId(), RedisPostCounter.BOOKMARK, post.getBookmarkCount()));
        resp.setHotScore(post.getHotScore());
        resp.setCreatedAt(post.getCreatedAt());

        // 作者
        UserEntity author = userMapper.selectOneById(post.getUserId());
        if (author != null) {
            UserProfileResp user = userService.buildProfile(author, currentUserId, false);
            resp.setUser(user);
        }

        // 媒体
        List<PostMediaEntity> medias = postMediaMapper.selectListByQuery(
                QueryWrapper.create().eq(PostMediaEntity::getPostId, post.getId()));
        resp.setMedias(medias.stream().map(this::toMediaResp).collect(Collectors.toList()));

        // @提及
        List<PostMentionEntity> mentions = postMentionMapper.selectListByQuery(
                QueryWrapper.create().eq(PostMentionEntity::getPostId, post.getId()));
        resp.setMentions(mentions.stream().map(this::toMentionResp).collect(Collectors.toList()));

        // 链接卡片
        PostLinkCardEntity card = postLinkCardMapper.selectOneByQuery(
                QueryWrapper.create().eq(PostLinkCardEntity::getPostId, post.getId()));
        resp.setLinkCard(card == null ? null : toLinkCardResp(card));

        // 当前用户视角：登录即可查真实状态（含本人，同推特）；游客一律 false
        boolean self = currentUserId != null && currentUserId.equals(post.getUserId());
        resp.setIsMine(self);
        if (currentUserId != null) {
            resp.setIsLiked(redisCounter.hasUser(post.getId(), RedisPostCounter.LIKE, currentUserId));
            resp.setIsBookmarked(redisCounter.hasUser(post.getId(), RedisPostCounter.BOOKMARK, currentUserId));
            resp.setIsReposted(redisCounter.hasUser(post.getId(), RedisPostCounter.REPOST, currentUserId));
        } else {
            resp.setIsLiked(false);
            resp.setIsBookmarked(false);
            resp.setIsReposted(false);
        }

        // 被引用/被回复的原文帖子（reply/quote/repost 时返回，限制一层避免深层递归）
        if (depth < 1 && post.getReferencedPostId() != null) {
            PostEntity ref = postMapper.selectOneById(post.getReferencedPostId());
            if (ref != null) {
                resp.setReferencedPost(buildPostResp(ref, currentUserId, depth + 1));
            }
        }
        return resp;
    }

    private PostMediaResp toMediaResp(PostMediaEntity m) {
        PostMediaResp r = new PostMediaResp();
        r.setId(m.getId());
        r.setMediaType(m.getMediaType());
        r.setOriginalUrl(m.getOriginalUrl());
        r.setThumbnailUrl(m.getThumbnailUrl());
        r.setPlayUrl(m.getPlayUrl());
        r.setWidth(m.getWidth());
        r.setHeight(m.getHeight());
        r.setDuration(m.getDuration());
        return r;
    }

    private PostMentionResp toMentionResp(PostMentionEntity m) {
        PostMentionResp r = new PostMentionResp();
        r.setId(m.getId());
        r.setMentionType(m.getMentionType());
        r.setTargetId(m.getTargetId());
        r.setStartIdx(m.getStartIdx());
        r.setEndIdx(m.getEndIdx());
        // user 提及：补充被 @ 用户的用户名与昵称，便于前端展示与跳转
        if (MentionTypeEnum.USER.getCode().equals(m.getMentionType()) && m.getTargetId() != null) {
            UserEntity u = userMapper.selectOneById(m.getTargetId());
            if (u != null) {
                r.setTargetUsername(u.getUsername());
                r.setTargetNickname(u.getNickname());
            }
        }
        return r;
    }

    private PostLinkCardResp toLinkCardResp(PostLinkCardEntity c) {
        PostLinkCardResp r = new PostLinkCardResp();
        r.setId(c.getId());
        r.setCardType(c.getCardType());
        r.setTargetUrl(c.getTargetUrl());
        r.setVideoProvider(c.getVideoProvider());
        r.setVideoId(c.getVideoId());
        r.setCoverMediaUrl(c.getCoverMediaUrl());
        return r;
    }

    /**
     * 可见性校验：followers_only 仅作者本人与关注者可见
     */
    private void checkViewPermission(PostEntity post, Long currentUserId) {
        if (currentUserId != null && currentUserId.equals(post.getUserId())) {
            return;
        }
        if (PostVisibilityEnum.FOLLOWERS_ONLY.getCode().equals(post.getVisibility())) {
            boolean following = userService.isFollowing(currentUserId, post.getUserId());
            if (!following) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "帖子不存在");
            }
        }
        // 作者拉黑了浏览者，则不可见
        if (userService.isBlocked(post.getUserId(), currentUserId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "帖子不存在");
        }
    }

    private PostEntity getActivePost(Long postId) {
        if (postId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "帖子ID不能为空");
        }
        PostEntity post = postMapper.selectOneById(postId);
        if (post == null || post.getDeletedAt() != null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "帖子不存在");
        }
        return post;
    }

    private Long resolveRootId(PostEntity referenced) {
        return referenced.getRootPostId() != null ? referenced.getRootPostId() : referenced.getId();
    }

    private void notify(Long receiverUserId, Long triggerUserId, String type, Long postId) {
        if (receiverUserId == null || receiverUserId.equals(triggerUserId)) {
            return;
        }
        notificationService.create(receiverUserId, triggerUserId, type, postId);
    }
}
