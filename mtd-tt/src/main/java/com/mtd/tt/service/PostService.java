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
import com.mtd.tt.mapper.UserFileMapper;
import com.mtd.tt.mapper.UserMapper;
import com.mtd.tt.mapper.UserTimelineMapper;
import com.mtd.tt.model.entity.BookmarkEntity;
import com.mtd.tt.model.entity.PostEntity;
import com.mtd.tt.model.entity.PostLikeEntity;
import com.mtd.tt.model.entity.PostLinkCardEntity;
import com.mtd.tt.model.entity.PostMediaEntity;
import com.mtd.tt.model.entity.PostMentionEntity;
import com.mtd.tt.model.entity.UserEntity;
import com.mtd.tt.model.entity.UserFileEntity;
import com.mtd.tt.model.entity.UserTimelineEntity;
import com.mtd.tt.model.req.ArticleListReq;
import com.mtd.tt.model.req.CreatePostLinkCardReq;
import com.mtd.tt.model.req.CreatePostMediaReq;
import com.mtd.tt.model.req.CreatePostMentionReq;
import com.mtd.tt.model.req.CreatePostReq;
import com.mtd.tt.model.req.ThreadReq;
import com.mtd.tt.model.resp.PostLinkCardResp;
import com.mtd.tt.model.resp.PostMediaResp;
import com.mtd.tt.model.resp.PostMentionResp;
import com.mtd.tt.model.resp.PostResp;
import com.mtd.tt.model.resp.RelationResp;
import com.mtd.tt.model.resp.ThreadResp;
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
import com.mtd.tt.util.HideMarkdown;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private UserFileMapper userFileMapper;
    @Autowired
    private LinkCardService linkCardService;
    @Autowired
    private TurnstileService turnstileService;
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
    @Autowired
    private PointService pointService;
    @Autowired
    private ExpService expService;
    @Autowired
    private HideContentService hideContentService;
    @Autowired
    private PostInteractService postInteractService;

    /**
     * 创建帖子（original / reply / quote / repost）
     */
    @Transactional(rollbackFor = Exception.class)
    public PostResp createPost(Long currentUserId, CreatePostReq req) {
        requireLogin(currentUserId);
        turnstileService.verify(req.getTurnstileToken());
        String type = StrUtil.isBlank(req.getType()) ? PostTypeEnum.ORIGINAL.getCode() : req.getType();
        PostTypeEnum typeEnum = PostTypeEnum.of(type);
        if (typeEnum == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不支持的帖子类型: " + type);
        }

        // 文章：带标题即视为长文（articleFlag=1），只允许 original，正文为 Markdown 原文
        String articleTitle = StrUtil.trim(req.getArticleTitle());
        boolean isArticle = StrUtil.isNotBlank(articleTitle);
        if (isArticle) {
            if (typeEnum != PostTypeEnum.ORIGINAL) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文章只能以 original 类型发布");
            }
            if (articleTitle.length() > 120) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文章标题最多 120 字");
            }
            if (StrUtil.isBlank(req.getContent())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文章正文不能为空");
            }
            if (req.getContent().length() > 20000) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文章正文最多 20000 字");
            }
        } else {
            articleTitle = "";
        }
        hideContentService.validateForCreate(req.getContent());

        // 互动类型只允许挂在原创主帖（推文/文章）上，回复/引用/转发固定为普通讨论
        PostInteractService.Normalized interact = typeEnum == PostTypeEnum.ORIGINAL
                ? postInteractService.normalize(req.getKind(), req.getInteract())
                : postInteractService.normalize("discussion", null);

        PostEntity referenced = null;
        if (typeEnum != PostTypeEnum.ORIGINAL) {
            if (req.getReferencedPostId() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "该类型帖子必须指定被引用帖子");
            }
            referenced = getActivePost(req.getReferencedPostId());
        }

        // 引用：按 Twitter 语义指向被引用这条推文本身，不回溯到最原始推文，也不限制次数；
        // 转发：只沿 repost 链归一（转推不能层层叠加），但不穿 quote——转推一条引用帖时目标就是那条引用帖；
        // 同一用户对同一转推目标只能转发一次。
        if (typeEnum == PostTypeEnum.REPOST) {
            referenced = resolveRepostRoot(referenced);
            Long rootId = referenced.getId();
            long dup = postMapper.selectCountByQuery(QueryWrapper.create()
                    .eq(PostEntity::getUserId, currentUserId)
                    .eq(PostEntity::getType, type)
                    .isNull(PostEntity::getDeletedAt)
                    .eq(PostEntity::getReferencedPostId, rootId));
            if (dup > 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "你已经转发过这条推文了");
            }
        }

        // 组装帖子
        PostEntity post = new PostEntity();
        post.setUserId(currentUserId);
        post.setType(type);
        post.setKind(interact.kind());
        post.setInteractMeta(interact.metaJson());
        post.setContent(req.getContent());
        if (typeEnum == PostTypeEnum.QUOTE || typeEnum == PostTypeEnum.REPOST) {
            // 引用：目标就是被引用那条推文；引用帖是一条独立推文，自己就是对话根，root 留空。
            // 转发：目标已归一化为被转推的推文（不穿 quote），root 与 referenced 同值。
            post.setReferencedPostId(referenced.getId());
            if (typeEnum == PostTypeEnum.REPOST) {
                post.setRootPostId(referenced.getId());
            }
        } else {
            post.setReferencedPostId(req.getReferencedPostId());
            // reply：root_post_id 指向引用帖的根
            if (typeEnum == PostTypeEnum.REPLY) {
                post.setRootPostId(resolveRootId(referenced));
            }
        }
        post.setVisibility(StrUtil.blankToDefault(req.getVisibility(), PostVisibilityEnum.PUBLIC.getCode()));
        post.setArticleFlag(isArticle ? 1 : 0);
        post.setArticleTitle(articleTitle);
        post.setArticleCoverUrl(StrUtil.blankToDefault(req.getArticleCoverUrl(), ""));
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
        // 发帖/转发/引用 扣 postCost，回复 扣 replyCost；积分不足则拒绝（在插入前拦截）
        pointService.deductForPost(currentUserId, type);
        expService.gainExp(currentUserId, type);
        postMapper.insertSelective(post);
        Long postId = post.getId();
        // 悬赏积分从楼主余额转入托管，与插入在同一事务，余额不足则整体回滚
        postInteractService.escrowBounty(currentUserId, post);

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
                // 客户端上传后回传文件 id，后端校验归属当前用户即可（不再按 url 匹配）
                Long originalFileId = resolveFileId(item.getOriginalFileId(), currentUserId);
                Long thumbnailFileId = resolveFileId(item.getThumbnailFileId(), currentUserId);
                Long playFileId = resolveFileId(item.getPlayFileId(), currentUserId);
                media.setOriginalFileId(originalFileId);
                media.setThumbnailFileId(thumbnailFileId);
                media.setPlayFileId(playFileId);
                postMediaMapper.insertSelective(media);
                // 回填文件归属推文
                bindFileToPost(originalFileId, currentUserId, postId);
                bindFileToPost(thumbnailFileId, currentUserId, postId);
                bindFileToPost(playFileId, currentUserId, postId);
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
        PostLinkCardEntity card = null;
        if (req.getLinkCard() != null) {
            card = new PostLinkCardEntity();
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
                card = linkCardService.resolve(cardUrl);
                if (card != null) {
                    card.setPostId(postId);
                    postLinkCardMapper.insertSelective(card);
                }
            }
        }
        // 链接卡片媒体并入媒体标记（图片卡片=link+封面 / 视频卡片=embed_video），使媒体类型筛选能匹配到含卡片的推文
        if (card != null) {
            boolean lcImage = CardTypeEnum.LINK.getCode().equals(card.getCardType())
                    && StrUtil.isNotBlank(card.getCoverMediaUrl());
            boolean lcVideo = CardTypeEnum.EMBED_VIDEO.getCode().equals(card.getCardType());
            if ((lcImage && !Boolean.TRUE.equals(post.getHasImage()))
                    || (lcVideo && !Boolean.TRUE.equals(post.getHasVideo()))) {
                if (lcImage) {
                    post.setHasImage(true);
                }
                if (lcVideo) {
                    post.setHasVideo(true);
                }
                postMapper.update(post);
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
     * 媒体类型过滤写进 SQL（globalTimeline / userPosts 走 wrapper）。
     * 基于 posts 表的 has_image/has_video（发帖时已把正文媒体与链接卡片媒体并入）。
     */
    private void applyMediaTypeFilter(QueryWrapper wrapper, String mediaType) {
        if ("image".equals(mediaType)) {
            wrapper.eq(PostEntity::getHasImage, true);
        } else if ("video".equals(mediaType)) {
            wrapper.eq(PostEntity::getHasVideo, true);
        }
    }

    /**
     * 媒体类型过滤（homeTimeline 已拉取 post 后逐条判断，需额外核对链接卡片）
     */
    private boolean matchMediaType(PostEntity post, String mediaType) {
        if ("image".equals(mediaType)) {
            if (Boolean.TRUE.equals(post.getHasImage())) {
                return true;
            }
            return hasLinkCardMedia(post.getId(), false);
        }
        if ("video".equals(mediaType)) {
            if (Boolean.TRUE.equals(post.getHasVideo())) {
                return true;
            }
            return hasLinkCardMedia(post.getId(), true);
        }
        return true;
    }

    /**
     * 判断该推文是否带图片卡片（cardType=link 且封面非空）或视频卡片（cardType=embed_video）
     */
    private boolean hasLinkCardMedia(Long postId, boolean video) {
        PostLinkCardEntity card = postLinkCardMapper.selectOneByQuery(
                QueryWrapper.create().eq(PostLinkCardEntity::getPostId, postId));
        if (card == null) {
            return false;
        }
        if (video) {
            return "embed_video".equals(card.getCardType());
        }
        return "link".equals(card.getCardType()) && StrUtil.isNotBlank(card.getCoverMediaUrl());
    }

    /**
     * 解析客户端回传的文件 id：校验该文件确属当前用户（user_files.id + user_id），
     * 非法或不存在则返回 null（不作为归属）。
     */
    private Long resolveFileId(Long fileId, Long userId) {
        if (fileId == null) {
            return null;
        }
        UserFileEntity file = userFileMapper.selectOneByQuery(
                QueryWrapper.create().eq(UserFileEntity::getId, fileId)
                        .eq(UserFileEntity::getUserId, userId));
        return file != null ? file.getId() : null;
    }

    /**
     * 文件回填归属推文（带用户校验，只更新当前用户自己的文件）。
     */
    private void bindFileToPost(Long fileId, Long userId, Long postId) {
        if (fileId == null) {
            return;
        }
        UserFileEntity upd = new UserFileEntity();
        upd.setPostId(postId);
        userFileMapper.updateByQuery(upd,
                QueryWrapper.create().eq(UserFileEntity::getId, fileId)
                        .eq(UserFileEntity::getUserId, userId));
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
        // 悬赏未结算时先把托管积分退回楼主，再软删除
        postInteractService.refundBounty(post);
        post.setDeletedAt(OffsetDateTime.now());
        postMapper.update(post);
        // 帖子删除后解除媒体文件绑定，媒体管理里才能正常删除这些文件
        UpdateChain.of(userFileMapper)
                .set(UserFileEntity::getPostId, null)
                .where(UserFileEntity::getPostId).eq(postId)
                .update();
        // 取消引用/取消转发 = 删除这条推文：把被引用原帖的转发计数与"已转发"关系同步减回去
        if ((PostTypeEnum.QUOTE.getCode().equals(post.getType())
                || PostTypeEnum.REPOST.getCode().equals(post.getType()))
                && post.getReferencedPostId() != null) {
            Long rootId = post.getRootPostId() != null ? post.getRootPostId() : post.getReferencedPostId();
            redisCounter.removeUser(rootId, RedisPostCounter.REPOST, currentUserId);
        }
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
     * 积分解锁隐藏内容：一次支付解锁本帖全部积分块。
     */
    @Transactional(rollbackFor = Exception.class)
    public PostResp unlockByPoints(Long currentUserId, Long postId) {
        requireLogin(currentUserId);
        PostEntity post = getActivePost(postId);
        checkViewPermission(post, currentUserId);
        hideContentService.unlockByPoints(postId, currentUserId);
        return buildPostResp(post, currentUserId);
    }

    /**
     * 密码解锁隐藏内容：游客也可用；登录用户与游客的解锁记录分别保存。
     */
    @Transactional(rollbackFor = Exception.class)
    public PostResp unlockByPassword(Long currentUserId, Long postId, String password, String guestToken) {
        PostEntity post = getActivePost(postId);
        checkViewPermission(post, currentUserId);
        if (currentUserId == null && StrUtil.isBlank(guestToken)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "无法建立访客会话，请登录后重试");
        }
        List<Integer> indexes = hideContentService.matchPassword(post.getContent(), password);
        hideContentService.savePasswordUnlocks(postId, currentUserId, guestToken, indexes);
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
    public PageResponse<PostResp> userPosts(Long targetUserId, PageRequest page, String postType, Long currentUserId, Long maxId, String mediaType) {
        userService.getProfile(targetUserId, currentUserId);
        QueryWrapper wrapper = QueryWrapper.create()
                .eq(PostEntity::getUserId, targetUserId)
                .isNull(PostEntity::getDeletedAt);
        if (StrUtil.isNotBlank(postType)) {
            wrapper.eq(PostEntity::getType, postType);
        }
        applyMediaTypeFilter(wrapper, mediaType);
        wrapper.orderBy(PostEntity::getId, false);
        return pagedPosts(wrapper, maxId, page.getPageSize(), currentUserId);
    }

    /**
     * 首页关注时间线：直接读 user_timeline（关注时回填近期推文，不做全量；看全量去用户主页）
     */
    public PageResponse<PostResp> homeTimeline(Long currentUserId, PageRequest page, Long maxId, String mediaType) {
        if (currentUserId == null) {
            // 未登录：关注流为空（全量最新推文走 globalTimeline）
            return PageResponse.of(page.getPageNum(), page.getPageSize(), 0L, List.of());
        }
        // 存量关注首次回填：时间线为空且有关注时，把已关注人的近期推文回填一次（之后靠关注/发帖增量维护）
        ensureTimelineBackfill(currentUserId);
        int pageSize = page.getPageSize();
        // 首页信息流只展示独立推文：回复沉在原帖详情里，避免评论插到原帖前面。
        // user_timeline 里可能残留历史回复，这里按批补齐到 pageSize 条有效推文再返回。
        Long cursor = maxId;
        int batchSize = Math.max(pageSize * 4, 40);
        List<PostResp> list = new ArrayList<>();
        while (list.size() < pageSize) {
            QueryWrapper tlWrapper = QueryWrapper.create()
                    .eq(UserTimelineEntity::getUserId, currentUserId)
                    .eq(UserTimelineEntity::getItemType, UserTimelineItemTypeEnum.POST.getCode())
                    .eq(UserTimelineEntity::getValid, Boolean.TRUE)
                    // 关注流不含自己的推文（同推特；历史残留的自己记录也一并过滤）
                    .ne(UserTimelineEntity::getItemUserId, currentUserId)
                    .orderBy(UserTimelineEntity::getItemId, false)
                    .limit(batchSize);
            if (cursor != null) {
                tlWrapper.lt(UserTimelineEntity::getItemId, cursor);
            }
            List<UserTimelineEntity> tls = userTimelineMapper.selectListByQuery(tlWrapper);
            if (CollUtil.isEmpty(tls)) {
                break;
            }
            for (UserTimelineEntity tl : tls) {
                cursor = tl.getItemId();
                PostEntity post = postMapper.selectOneById(tl.getItemId());
                if (post == null || post.getDeletedAt() != null) {
                    continue;
                }
                if (PostTypeEnum.REPLY.getCode().equals(post.getType())) {
                    continue;
                }
                if (!matchMediaType(post, mediaType)) {
                    continue;
                }
                try {
                    checkViewPermission(post, currentUserId);
                } catch (BusinessException e) {
                    continue;
                }
                list.add(buildPostResp(post, currentUserId));
                if (list.size() >= pageSize) {
                    break;
                }
            }
            if (tls.size() < batchSize) {
                break;
            }
        }
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
    public PageResponse<PostResp> globalTimeline(Long currentUserId, PageRequest page, Long maxId, String mediaType) {
        QueryWrapper wrapper = QueryWrapper.create()
                .isNull(PostEntity::getDeletedAt)
                // 推荐流只展示独立推文，回复留在原帖详情里
                .ne(PostEntity::getType, PostTypeEnum.REPLY.getCode())
                .orderByUnSafely("(hot_score / POWER(EXTRACT(EPOCH FROM (now() - created_at)) / 3600.0 + 2, 1.8)) DESC, id DESC");
        applyMediaTypeFilter(wrapper, mediaType);
        return pagedPosts(wrapper, maxId, page.getPageSize(), currentUserId);
    }

    /**
     * 文章列表：发现（全站 articleFlag=1 的 original 长文）/ 我的作品，按发布时间倒序分页。
     */
    public PageResponse<PostResp> articles(ArticleListReq req, Long currentUserId) {
        int pageNum = req.getPageNum() == null || req.getPageNum() <= 0 ? 1 : req.getPageNum();
        int pageSize = req.getPageSize() == null || req.getPageSize() <= 0 ? 20 : Math.min(req.getPageSize(), 100);
        boolean mine = "mine".equalsIgnoreCase(StrUtil.blankToDefault(req.getScope(), "all"));
        QueryWrapper wrapper = QueryWrapper.create()
                .eq(PostEntity::getArticleFlag, 1)
                .eq(PostEntity::getType, PostTypeEnum.ORIGINAL.getCode())
                .isNull(PostEntity::getDeletedAt);
        if (mine) {
            requireLogin(currentUserId);
            wrapper.eq(PostEntity::getUserId, currentUserId);
        }
        wrapper.orderBy(PostEntity::getCreatedAt, false).orderBy(PostEntity::getId, false);
        Page<PostEntity> p = postMapper.paginate(pageNum, pageSize, wrapper);
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
     * 详情页对话视图（推特式）：当前帖 + 上文链 + 整条对话的回复（平铺，可按 最相关/最新/最多点赞 排序）。
     * <p>上文链只沿 reply 的 referencedPostId 上溯；引用/转发用引用卡表达，不展开成上下文。
     * 回复以对话根（root_post_id）为范围平铺，pageNum 分页；排除当前帖和上方祖先，避免重复。
     */
    public ThreadResp thread(ThreadReq req, Long currentUserId) {
        PostEntity post = getActivePost(req.getPostId());
        int pageSize = req.getPageSize() == null || req.getPageSize() <= 0 ? 20 : Math.min(req.getPageSize(), 100);
        int pageNum = req.getPageNum() == null || req.getPageNum() <= 0 ? 1 : req.getPageNum();

        List<PostResp> ancestorResps = ancestors(post, currentUserId);

        // 对话根：reply/repost 取 rootPostId，original/quote 自身即根
        Long rootId = post.getRootPostId() != null && post.getRootPostId() > 0 ? post.getRootPostId() : post.getId();
        List<Long> exclude = new ArrayList<>();
        exclude.add(post.getId());
        for (PostResp a : ancestorResps) {
            exclude.add(a.getId());
        }

        QueryWrapper countWrapper = QueryWrapper.create()
                .eq(PostEntity::getType, PostTypeEnum.REPLY.getCode())
                .eq(PostEntity::getRootPostId, rootId)
                .isNull(PostEntity::getDeletedAt)
                .notIn(PostEntity::getId, exclude);
        Long total = postMapper.selectCountByQuery(countWrapper);

        QueryWrapper wrapper = QueryWrapper.create()
                .eq(PostEntity::getType, PostTypeEnum.REPLY.getCode())
                .eq(PostEntity::getRootPostId, rootId)
                .isNull(PostEntity::getDeletedAt)
                .notIn(PostEntity::getId, exclude);
        applySort(wrapper, req.getSort(), false);
        wrapper.offset((pageNum - 1) * pageSize).limit(pageSize + 1);
        List<PostEntity> rows = postMapper.selectListByQuery(wrapper);
        boolean hasMore = rows.size() > pageSize;
        if (hasMore) {
            rows = rows.subList(0, pageSize);
        }

        List<PostResp> replies = new ArrayList<>();
        for (PostEntity p : rows) {
            try {
                checkViewPermission(p, currentUserId);
            } catch (BusinessException e) {
                continue;
            }
            replies.add(buildPostResp(p, currentUserId));
        }
        fillReplyToUsernames(replies);

        // 引用数、转推数都按实际关系记录统计（帖子自带的 repostCount 是热度累计，可能包含已删的）
        Long quoteCount = postMapper.selectCountByQuery(QueryWrapper.create()
                .eq(PostEntity::getType, PostTypeEnum.QUOTE.getCode())
                .eq(PostEntity::getReferencedPostId, post.getId())
                .isNull(PostEntity::getDeletedAt));
        Long repostCount = postMapper.selectCountByQuery(QueryWrapper.create()
                .eq(PostEntity::getType, PostTypeEnum.REPOST.getCode())
                .eq(PostEntity::getRootPostId, post.getId())
                .isNull(PostEntity::getDeletedAt));

        ThreadResp resp = new ThreadResp();
        resp.setPost(buildPostResp(post, currentUserId));
        resp.setAncestors(ancestorResps);
        resp.setReplies(replies);
        resp.setTotal(total);
        resp.setHasMore(hasMore);
        resp.setPageNum(pageNum);
        resp.setQuoteCount(quoteCount);
        resp.setRepostCount(repostCount);
        return resp;
    }

    /**
     * 排序映射：latest → 时间倒序；liked → 点赞倒序；默认 relevant → 互动加权倒序。
     * <p>relevant 不再直接依赖 hot_score：该字段可能未回填或为 0，会导致排序退化成 id 倒序。
     * 分数相同时按时间正序，零互动的新回复沉到后面，不再抢占首屏；要看最新在前请切到 latest。
     * 权重：转发 30 > 收藏 15 > 点赞 10 > 回复 1；二级及更深的回复再减 2，避免空评论插队。
     */
    private void applySort(QueryWrapper wrapper, String sort, boolean ascDefault) {
        if ("latest".equals(sort)) {
            wrapper.orderBy(PostEntity::getCreatedAt, false).orderBy(PostEntity::getId, false);
        } else if ("liked".equals(sort)) {
            wrapper.orderBy(PostEntity::getLikeCount, false);
            wrapper.orderByUnSafely(
                    "(bookmark_count * 15 + repost_count * 30 + reply_count" +
                            " - CASE WHEN referenced_post_id = root_post_id THEN 0 ELSE 2 END)" +
                            " DESC, created_at DESC, id DESC");
        } else if (sort == null || "relevant".equals(sort)) {
            wrapper.orderByUnSafely(
                    "(like_count * 10 + bookmark_count * 15 + repost_count * 30 + reply_count" +
                            " - CASE WHEN referenced_post_id = root_post_id THEN 0 ELSE 2 END)" +
                            " DESC, created_at ASC, id ASC");
        } else {
            wrapper.orderBy(PostEntity::getId, ascDefault);
        }
    }

    /**
     * 某条推文的引用列表 / 转推列表（详情页「查看引用」「查看转推」）
     */
    public RelationResp relations(Long postId, String type, String sort, Integer pageNumReq, Integer pageSizeReq, Long currentUserId) {
        getActivePost(postId);
        int pageSize = pageSizeReq == null || pageSizeReq <= 0 ? 20 : Math.min(pageSizeReq, 100);
        int pageNum = pageNumReq == null || pageNumReq <= 0 ? 1 : pageNumReq;
        boolean repost = "repost".equals(type);

        QueryWrapper countWrapper = QueryWrapper.create()
                .eq(PostEntity::getType, repost ? PostTypeEnum.REPOST.getCode() : PostTypeEnum.QUOTE.getCode())
                .isNull(PostEntity::getDeletedAt);
        if (repost) {
            countWrapper.eq(PostEntity::getRootPostId, postId);
        } else {
            countWrapper.eq(PostEntity::getReferencedPostId, postId);
        }
        Long total = postMapper.selectCountByQuery(countWrapper);

        QueryWrapper wrapper = QueryWrapper.create()
                .eq(PostEntity::getType, repost ? PostTypeEnum.REPOST.getCode() : PostTypeEnum.QUOTE.getCode())
                .isNull(PostEntity::getDeletedAt);
        if (repost) {
            wrapper.eq(PostEntity::getRootPostId, postId);
        } else {
            wrapper.eq(PostEntity::getReferencedPostId, postId);
        }
        applySort(wrapper, sort, false);
        wrapper.offset((pageNum - 1) * pageSize).limit(pageSize + 1);
        List<PostEntity> rows = postMapper.selectListByQuery(wrapper);
        boolean hasMore = rows.size() > pageSize;
        if (hasMore) {
            rows = rows.subList(0, pageSize);
        }

        List<PostResp> list = new ArrayList<>();
        for (PostEntity p : rows) {
            try {
                checkViewPermission(p, currentUserId);
            } catch (BusinessException e) {
                continue;
            }
            list.add(buildPostResp(p, currentUserId));
        }

        RelationResp resp = new RelationResp();
        resp.setList(list);
        resp.setTotal(total);
        resp.setHasMore(hasMore);
        resp.setPageNum(pageNum);
        return resp;
    }

    /**
     * 给平铺回复补上「回复给谁」：父帖作者的 username（批量查父帖与用户，避免 N+1）
     */
    private void fillReplyToUsernames(List<PostResp> replies) {
        List<Long> parentIds = replies.stream()
                .map(PostResp::getReferencedPostId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (parentIds.isEmpty()) {
            return;
        }
        QueryWrapper parentWrapper = QueryWrapper.create()
                .in(PostEntity::getId, parentIds)
                .select(PostEntity::getId, PostEntity::getUserId);
        List<PostEntity> parents = postMapper.selectListByQuery(parentWrapper);
        List<Long> userIds = parents.stream().map(PostEntity::getUserId).distinct().collect(Collectors.toList());
        QueryWrapper userWrapper = QueryWrapper.create()
                .in(UserEntity::getId, userIds)
                .select(UserEntity::getId, UserEntity::getUsername);
        List<UserEntity> users = userMapper.selectListByQuery(userWrapper);
        java.util.Map<Long, Long> parentUser = parents.stream()
                .collect(Collectors.toMap(PostEntity::getId, PostEntity::getUserId, (a, b) -> a));
        java.util.Map<Long, String> userNames = users.stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getUsername, (a, b) -> a));
        for (PostResp r : replies) {
            Long parentUserId = r.getReferencedPostId() == null ? null : parentUser.get(r.getReferencedPostId());
            if (parentUserId != null) {
                r.setReplyToUsername(userNames.get(parentUserId));
            }
        }
    }

    /**
     * 上文链：从当前帖沿 reply 的 referencedPostId 上溯，返回 root → parent 顺序（深度上限 10、防环）
     */
    private List<PostResp> ancestors(PostEntity post, Long currentUserId) {
        List<PostEntity> chain = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        PostEntity cur = post;
        for (int i = 0; i < 10; i++) {
            if (!PostTypeEnum.REPLY.getCode().equals(cur.getType()) || cur.getReferencedPostId() == null) {
                break;
            }
            Long pid = cur.getReferencedPostId();
            if (seen.contains(pid)) {
                break;
            }
            seen.add(pid);
            PostEntity parent = postMapper.selectOneById(pid);
            if (parent == null || parent.getDeletedAt() != null) {
                break;
            }
            chain.add(parent);
            cur = parent;
        }
        Collections.reverse(chain);
        return chain.stream().map(p -> buildPostResp(p, currentUserId)).collect(Collectors.toList());
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
        resp.setKind(postInteractService.kindOf(post));
        // 互动状态按当前读者计算；问答/悬赏的采纳回复在下面补成完整卡片
        java.util.Map<String, Object> interactView = postInteractService.buildView(post, currentUserId);
        resp.setInteract(interactView);
        // 隐藏内容按当前读者能力由服务端脱敏；未解锁正文不会随响应下发
        HideMarkdown.Sanitized sanitized =
                hideContentService.sanitize(post.getContent(), post.getId(), post.getUserId(), currentUserId);
        resp.setContent(sanitized.content);
        resp.setHideBlocks(hideContentService.toRespList(sanitized.statuses));
        resp.setReferencedPostId(post.getReferencedPostId());
        resp.setRootPostId(post.getRootPostId());
        resp.setVisibility(post.getVisibility());
        resp.setArticleFlag(post.getArticleFlag());
        resp.setArticleTitle(post.getArticleTitle());
        resp.setArticleCoverUrl(post.getArticleCoverUrl());
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
        resp.setMine(self);
        if (currentUserId != null) {
            resp.setLiked(redisCounter.hasUser(post.getId(), RedisPostCounter.LIKE, currentUserId));
            resp.setBookmarked(redisCounter.hasUser(post.getId(), RedisPostCounter.BOOKMARK, currentUserId));
            resp.setReposted(redisCounter.hasUser(post.getId(), RedisPostCounter.REPOST, currentUserId));
        } else {
            resp.setLiked(false);
            resp.setBookmarked(false);
            resp.setReposted(false);
        }

        // 被引用/被回复的原文帖子：最多两层。
        // 转推一条引用帖时，第一层是被转推的那条 quote，第二层才是它引用的原推；
        // 两层之后不再往下取（depth >= 2 停止），避免深层递归。
        if (depth < 2 && post.getReferencedPostId() != null) {
            PostEntity ref = postMapper.selectOneById(post.getReferencedPostId());
            if (ref != null && ref.getDeletedAt() == null) {
                resp.setReferencedPost(buildPostResp(ref, currentUserId, depth + 1));
            }
        }
        // 采纳回答：只补一层摘要卡片，depth=2 不再取它引用的提问，避免循环
        Object accepted = interactView.get("acceptedReplyId");
        if (accepted instanceof Number n && n.longValue() > 0) {
            PostEntity reply = postMapper.selectOneById(n.longValue());
            if (reply != null && reply.getDeletedAt() == null) {
                PostResp acceptedResp = buildPostResp(reply, currentUserId, 2);
                acceptedResp.setReferencedPost(null);
                interactView.put("acceptedReply", acceptedResp);
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
        if (userService.blocked(post.getUserId(), currentUserId)) {
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

    /**
     * 把被转推的推文归一化为“那条被转推的推文”（推特不允许转推的转推）。
     * 只沿 repost 链回溯：遇到 quote / original / reply 立即停在它自身——
     * 引用帖是一条独立推文，转推它时目标就是这条引用帖，不往上穿到它引用的原推。
     * 深度上限 10、seen 防环。
     */
    private PostEntity resolveRepostRoot(PostEntity start) {
        PostEntity cur = start;
        Set<Long> seen = new HashSet<>();
        seen.add(cur.getId());
        for (int i = 0; i < 10; i++) {
            if (!PostTypeEnum.REPOST.getCode().equals(cur.getType())) {
                return cur;
            }
            Long nextId = cur.getReferencedPostId();
            if (nextId == null || nextId <= 0 || seen.contains(nextId)) {
                return cur;
            }
            PostEntity next = postMapper.selectOneById(nextId);
            if (next == null || next.getDeletedAt() != null) {
                return cur;
            }
            seen.add(nextId);
            cur = next;
        }
        return cur;
    }

    private void notify(Long receiverUserId, Long triggerUserId, String type, Long postId) {
        if (receiverUserId == null || receiverUserId.equals(triggerUserId)) {
            return;
        }
        notificationService.create(receiverUserId, triggerUserId, type, postId);
    }
}
