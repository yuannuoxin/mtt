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
import com.mtd.tt.mapper.UserTimelineMapper;
import com.mtd.tt.model.entity.BookmarkEntity;
import com.mtd.tt.model.entity.PostEntity;
import com.mtd.tt.model.entity.PostLikeEntity;
import com.mtd.tt.model.entity.PostLinkCardEntity;
import com.mtd.tt.model.entity.PostMediaEntity;
import com.mtd.tt.model.entity.PostMentionEntity;
import com.mtd.tt.model.entity.UserEntity;
import com.mtd.tt.model.entity.UserTimelineEntity;
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
import com.mtd.tt.enums.HotScoreWeightEnum;
import com.mtd.tt.enums.MediaTypeEnum;
import com.mtd.tt.enums.MentionTypeEnum;
import com.mtd.tt.enums.NotificationTypeEnum;
import com.mtd.tt.enums.PostTypeEnum;
import com.mtd.tt.enums.PostTypeWeightEnum;
import com.mtd.tt.enums.PostVisibilityEnum;
import com.mtd.tt.enums.ReplyPermissionEnum;
import com.mtd.tt.enums.UserTimelineItemTypeEnum;
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

    /**
     * 热度落库挡位：互动热度先在 Redis 累计，达到该阈值才写一次 DB。
     * 点赞权重10分，即约10次点赞才触发一次落库，降低高频互动对数据库的写压力。
     */
    private static final long HOT_FLUSH_THRESHOLD = 100;

    @Autowired
    private PostMapper postMapper;
    @Autowired
    private PostMediaMapper postMediaMapper;
    @Autowired
    private PostMentionMapper postMentionMapper;
    @Autowired
    private PostLinkCardMapper postLinkCardMapper;
    @Autowired
    private LinkCardService linkCardService;
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
    @Autowired
    private UserTimelineMapper userTimelineMapper;
    @Autowired
    private TimelineService timelineService;

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
        // 基础热度 = 帖子类型权重（原帖>引用>回复>转发），互动在此基础上累加；
        // 排序时再叠加时间衰减，避免回复/转发这类低价值内容占据太多展示位
        post.setHotScore(PostTypeWeightEnum.of(type).getBase());
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

        // 链接卡片：优先使用调用方显式传入的卡片；否则从正文自动提取链接并解析生成
        if (req.getLinkCard() != null) {
            PostLinkCardEntity card = new PostLinkCardEntity();
            card.setPostId(postId);
            card.setCardType(StrUtil.blankToDefault(req.getLinkCard().getCardType(), CardTypeEnum.LINK.getCode()));
            card.setTargetUrl(req.getLinkCard().getTargetUrl());
            card.setTitle(req.getLinkCard().getTitle());
            card.setDescription(req.getLinkCard().getDescription());
            card.setVideoProvider(req.getLinkCard().getVideoProvider());
            card.setVideoId(req.getLinkCard().getVideoId());
            card.setCoverMediaUrl(req.getLinkCard().getCoverMediaUrl());
            postLinkCardMapper.insertSelective(card);
        } else {
            String cardUrl = linkCardService.extractUrl(req.getContent());
            if (StrUtil.isNotBlank(cardUrl)) {
                PostLinkCardEntity card = linkCardService.resolve(cardUrl);
                if (card != null) {
                    card.setPostId(postId);
                    postLinkCardMapper.insertSelective(card);
                }
            }
        }

        // 更新被引用帖计数 + 热度 + 生成通知（计数走 Redis，热度走 DB 累加）
        if (referenced != null) {
            switch (typeEnum) {
                case REPLY -> {
                    redisCounter.incr(referenced.getId(), RedisPostCounter.REPLY);
                    addHot(referenced.getId(), HotScoreWeightEnum.REPLY.getWeight());
                    notify(referenced.getUserId(), currentUserId, NotificationTypeEnum.COMMENT.getCode(), referenced.getId());
                }
                case QUOTE, REPOST -> {
                    if (typeEnum == PostTypeEnum.REPOST) {
                        redisCounter.addUser(referenced.getId(), RedisPostCounter.REPOST, currentUserId); // SADD + 计数 +1
                    } else {
                        redisCounter.incr(referenced.getId(), RedisPostCounter.REPOST); // quote 仅计数 +1
                    }
                    addHot(referenced.getId(), HotScoreWeightEnum.REPOST.getWeight());
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

        // 发帖成功后推送到作者全部粉丝的时间线（fanout）
        timelineService.pushOnPostCreated(post);

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
        // 从所有时间线里移除该帖
        timelineService.clearOnPostDeleted(postId);
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
     * 某用户的帖子列表（id 倒序 + 游标分页）
     */
    public PageResponse<PostResp> userPosts(Long targetUserId, PageRequest page, String postType, Long currentUserId, Long maxId) {
        userService.getProfile(targetUserId, currentUserId);
        QueryWrapper wrapper = QueryWrapper.create()
                .eq(PostEntity::getUserId, targetUserId)
                .isNull(PostEntity::getDeletedAt);
        if (StrUtil.isNotBlank(postType)) {
            wrapper.eq(PostEntity::getType, postType);
        }
        wrapper.orderBy(PostEntity::getId, false);
        return pagedPosts(wrapper, maxId, page.getPageSize(), currentUserId);
    }

    /**
     * 首页关注时间线：直接读 user_timeline（关注时回填近期推文，不做全量；看全量去用户主页）
     */
    public PageResponse<PostResp> homeTimeline(Long currentUserId, PageRequest page, Long maxId) {
        if (currentUserId == null) {
            // 未登录：关注流为空（全量最新推文走 globalTimeline）
            return PageResponse.of(page.getPageNum(), page.getPageSize(), 0L, List.of());
        }
        // 存量关注首次回填：时间线为空且有关注时，把已关注人的近期推文回填一次（之后靠关注/发帖增量维护）
        ensureTimelineBackfill(currentUserId);
        int pageSize = page.getPageSize();
        QueryWrapper tlWrapper = QueryWrapper.create()
                .eq(UserTimelineEntity::getUserId, currentUserId)
                .eq(UserTimelineEntity::getItemType, UserTimelineItemTypeEnum.POST.getCode())
                .eq(UserTimelineEntity::getValid, Boolean.TRUE)
                // 关注流不含自己的推文（同推特；历史残留的自己记录也一并过滤）
                .ne(UserTimelineEntity::getItemUserId, currentUserId)
                .orderBy(UserTimelineEntity::getItemId, false)
                .limit(pageSize);
        if (maxId != null) {
            tlWrapper.lt(UserTimelineEntity::getItemId, maxId);
        }
        List<UserTimelineEntity> tls = userTimelineMapper.selectListByQuery(tlWrapper);
        List<PostResp> list = tls.stream().map(tl -> {
                    PostEntity post = postMapper.selectOneById(tl.getItemId());
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
        return PageResponse.of(page.getPageNum(), page.getPageSize(), (long) list.size(), list);
    }

    /**
     * 存量关注回填：时间线为空且有关注关系时，把已关注人（含自己）的近期推文回填一次。
     * 之后时间线靠关注时回填 + 发帖时推送增量维护，不重复触发。
     */
    private void ensureTimelineBackfill(Long userId) {
        long cnt = userTimelineMapper.selectCountByQuery(
                QueryWrapper.create().eq(UserTimelineEntity::getUserId, userId));
        if (cnt > 0) {
            return;
        }
        List<Long> ids = userService.followingUserIds(userId); // 关注的人 + 自己
        // 关注流与推特一致：回填只取关注的人，排除自己
        ids.remove(userId);
        for (Long id : ids) {
            timelineService.fillOnFollow(userId, id);
        }
    }

    /**
     * 全量推荐推文（按热度 = 类型权重+互动，叠加时间衰减倒序；id 兜底）
     * 回复/转发基础热度低，天然排在后面，避免首页被回复等低价值内容占据。
     */
    public PageResponse<PostResp> globalTimeline(Long currentUserId, PageRequest page, Long maxId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .isNull(PostEntity::getDeletedAt)
                .orderByUnSafely("(hot_score / POWER(EXTRACT(EPOCH FROM (now() - created_at)) / 3600.0 + 2, 1.8)) DESC, id DESC");
        return pagedPosts(wrapper, maxId, page.getPageSize(), currentUserId);
    }

    /**
     * 某帖子的回复列表（id 正序分页）
     */
    public PageResponse<PostResp> replies(Long postId, Long currentUserId, PageRequest page) {
        getActivePost(postId);
        QueryWrapper wrapper = QueryWrapper.create()
                .eq(PostEntity::getReferencedPostId, postId)
                .eq(PostEntity::getType, PostTypeEnum.REPLY.getCode())
                .isNull(PostEntity::getDeletedAt)
                .orderBy(PostEntity::getId, true);
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
     * 搜索帖子（正文模糊匹配，热度按"互动分 + 时间衰减"倒序 + 游标分页）
     * <p>排序分 = hot_score / (距发布小时数 + 2)^1.8：
     * 互动越多、越新越靠前；很久以前的数据即使曾很热，也会随时间指数衰减。
     */
    public PageResponse<PostResp> search(String keyword, Long currentUserId, PageRequest page, Long maxId) {
        if (StrUtil.isBlank(keyword)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "搜索关键词不能为空");
        }
        QueryWrapper wrapper = QueryWrapper.create()
                .like(PostEntity::getContent, keyword)
                .isNull(PostEntity::getDeletedAt)
                .orderByUnSafely("(hot_score / POWER(EXTRACT(EPOCH FROM (now() - created_at)) / 3600.0 + 2, 1.8)) DESC, id DESC");
        return pagedPosts(wrapper, maxId, page.getPageSize(), currentUserId);
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
        addHot(postId, HotScoreWeightEnum.LIKE.getWeight()); // 热度挡位累加
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
        addHot(postId, -HotScoreWeightEnum.LIKE.getWeight()); // 热度回退
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
        addHot(postId, HotScoreWeightEnum.BOOKMARK.getWeight()); // 热度挡位累加
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
        addHot(postId, -HotScoreWeightEnum.BOOKMARK.getWeight()); // 热度回退
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

    /**
     * 游标分页：按 id 倒序取一页（maxId 为上一页最后一条的 id，取更早一页）。
     * 不依赖传统 offset 分页，返回条数即分页判断依据（list.size() == pageSize 则还有更多）。
     */
    private PageResponse<PostResp> pagedPosts(QueryWrapper wrapper, Long maxId, int pageSize, Long currentUserId) {
        if (maxId != null) {
            wrapper.lt(PostEntity::getId, maxId);
        }
        wrapper.limit(pageSize);
        List<PostEntity> records = postMapper.selectListByQuery(wrapper);
        List<PostResp> list = records.stream()
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
        return PageResponse.of(1, pageSize, (long) list.size(), list);
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
     * 互动热度挡位落库：先在 Redis 累计增量，达到阈值才写一次 DB 并清零，降低高频互动的写库压力。
     */
    private void addHot(Long postId, long delta) {
        long acc = redisCounter.incrHot(postId, delta);
        if (Math.abs(acc) >= HOT_FLUSH_THRESHOLD) {
            Long flushed = redisCounter.getAndResetHot(postId);
            if (flushed != null && flushed != 0) {
                postMapper.updateHotScore(postId, flushed);
            }
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
        r.setTitle(c.getTitle());
        r.setDescription(c.getDescription());
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
