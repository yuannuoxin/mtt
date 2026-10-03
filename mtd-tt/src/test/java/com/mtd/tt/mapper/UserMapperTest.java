package com.mtd.tt.mapper;

import cn.hutool.json.JSONUtil;
import com.mtd.tt.model.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ActiveProfiles("local")
@SpringBootTest
public class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void t1() {
        // 测试 1：查询数据库
        userMapper.selectAll();
    }

    @Rollback(false)
    @Test
    public void t2() {
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(UUID.randomUUID().toString());
        userEntity.setEmail(UUID.randomUUID().toString());
        userEntity.setPasswordHash(UUID.randomUUID().toString());
        userEntity.setNickname(UUID.randomUUID().toString());
        userEntity.setAvatar(UUID.randomUUID().toString());
        userEntity.setBanner(UUID.randomUUID().toString());
        userEntity.setBio(UUID.randomUUID().toString());
        userEntity.setStatus("active");
        userEntity.setCreatedAt(OffsetDateTime.now());
        userEntity.setUpdatedAt(OffsetDateTime.now());
        userMapper.insert(userEntity);

        // 测试 1：查询数据库
        List<UserEntity> userEntities = userMapper.selectAll();
        System.out.println(JSONUtil.toJsonPrettyStr(userEntities));
    }

    @Autowired
    FollowMapper followMapper;

    @Rollback(false)
    @Test
    public void t3() {
        FollowEntity followEntity = new FollowEntity();
        followEntity.setFollowerId(1L);
        followEntity.setFolloweeId(2L);
        followEntity.setCreatedAt(OffsetDateTime.now());

        followMapper.insert(followEntity);

        // 测试 1：查询数据库
        List<FollowEntity> followEntities = followMapper.selectAll();
        System.out.println(JSONUtil.toJsonPrettyStr(followEntities));
    }

    @Autowired
    BlockMapper blockMapper;

    @Rollback(false)
    @Test
    public void t4() {
        BlockEntity blockEntity = new BlockEntity();
        blockEntity.setUserId(1L);
        blockEntity.setBlockedUserId(2L);
        blockEntity.setCreatedAt(OffsetDateTime.now());
        blockMapper.insert(blockEntity);

        // 测试 1：查询数据库
        List<BlockEntity> blockEntities = blockMapper.selectAll();
        System.out.println(JSONUtil.toJsonPrettyStr(blockEntities));
    }

    @Autowired
    PostMapper postMapper;

    @Rollback(false)
    @Test
    public void t5() {
        PostEntity postEntity = new PostEntity();
        postEntity.setUserId(1L);
        postEntity.setType("");
        postEntity.setContent("");
        postEntity.setReferencedPostId(null);
        postEntity.setRootPostId(null);
        postEntity.setVisibility("public");
        postEntity.setArticleFlag(0);
        postEntity.setHasMedia(false);
        postEntity.setHasImage(false);
        postEntity.setHasVideo(false);
        postEntity.setReplyPermission("anyone");
        postEntity.setLikeCount(0L);
        postEntity.setRepostCount(0L);
        postEntity.setReplyCount(0L);
        postEntity.setViewCount(0L);
        postEntity.setBookmarkCount(0L);
        postEntity.setHotScore(0L);
        postEntity.setCreatedAt(OffsetDateTime.now());
        postEntity.setUpdatedAt(OffsetDateTime.now());

        postMapper.insert(postEntity);

        // 测试 1：查询数据库
        List<PostEntity> postEntities = postMapper.selectAll();
        System.out.println(JSONUtil.toJsonPrettyStr(postEntities));
    }


    @Autowired
    PostMediaMapper postMediaMapper;

    @Rollback(false)
    @Test
    public void t6() {
        PostMediaEntity postMediaEntity = new PostMediaEntity();
        postMediaEntity.setPostId(1L);
        postMediaEntity.setMediaType("image");
        postMediaEntity.setOriginalUrl("");
        postMediaEntity.setThumbnailUrl("");
        postMediaEntity.setPlayUrl("");
        postMediaEntity.setWidth(0);
        postMediaEntity.setHeight(0);
        postMediaEntity.setDuration(new BigDecimal("0"));
        postMediaEntity.setCreatedAt(OffsetDateTime.now());


        postMediaMapper.insert(postMediaEntity);

        // 测试 1：查询数据库
        List<PostMediaEntity> postMediaEntities = postMediaMapper.selectAll();
        System.out.println(JSONUtil.toJsonPrettyStr(postMediaEntities));
    }

    @Autowired
    PostMentionMapper postMentionMapper;

    @Rollback(false)
    @Test
    public void t7() {
        PostMentionEntity postMentionEntity = new PostMentionEntity();
        postMentionEntity.setPostId(1L);
        postMentionEntity.setMentionType("user");
        postMentionEntity.setTargetId(0L);
        postMentionEntity.setStartIdx(0L);
        postMentionEntity.setEndIdx(0L);
        postMentionEntity.setCreatedAt(OffsetDateTime.now());

        postMentionMapper.insert(postMentionEntity);

        // 测试 1：查询数据库
        List<PostMentionEntity> postMentionEntities = postMentionMapper.selectAll();
        System.out.println(JSONUtil.toJsonPrettyStr(postMentionEntities));
    }

    @Autowired
    PostLikeMapper postLikeMapper;

    @Rollback(false)
    @Test
    public void t8() {
        PostLikeEntity postLikeEntity = new PostLikeEntity();
        postLikeEntity.setUserId(0L);
        postLikeEntity.setPostId(0L);
        postLikeEntity.setCreatedAt(OffsetDateTime.now());

        postLikeMapper.insert(postLikeEntity);

        // 测试 1：查询数据库
        List<PostLikeEntity> postLikeEntityList = postLikeMapper.selectAll();
        System.out.println(JSONUtil.toJsonPrettyStr(postLikeEntityList));
    }

    @Autowired
    BookmarkMapper bookmarkMapper;

    @Rollback(false)
    @Test
    public void t9() {
        BookmarkEntity bookmarkEntity = new BookmarkEntity();
        bookmarkEntity.setUserId(0L);
        bookmarkEntity.setPostId(0L);
        bookmarkEntity.setCreatedAt(OffsetDateTime.now());

        bookmarkMapper.insert(bookmarkEntity);

        // 测试 1：查询数据库
        List<BookmarkEntity> bookmarkEntityList = bookmarkMapper.selectAll();
        System.out.println(JSONUtil.toJsonPrettyStr(bookmarkEntityList));
    }

    @Autowired
    NotificationMapper notificationMapper;

    @Rollback(false)
    @Test
    public void t10() {
        NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setReceiverUserId(0L);
        notificationEntity.setTriggerUserId(0L);
        notificationEntity.setType("like");
        notificationEntity.setPostId(0L);
        notificationEntity.setRead(false);
        notificationEntity.setCreatedAt(OffsetDateTime.now());


        notificationMapper.insert(notificationEntity);

        // 测试 1：查询数据库
        List<NotificationEntity> notificationEntityList = notificationMapper.selectAll();
        System.out.println(JSONUtil.toJsonPrettyStr(notificationEntityList));
    }

    @Autowired
    UserTimelineMapper userTimelineMapper;

    @Rollback(false)
    @Test
    public void t11() {
        UserTimelineEntity userTimelineEntity = new UserTimelineEntity();
        userTimelineEntity.setUserId(0L);
        userTimelineEntity.setItemType("");
        userTimelineEntity.setItemId(0L);
        userTimelineEntity.setItemUserId(0L);
        userTimelineEntity.setValid(false);
        userTimelineEntity.setCreatedAt(OffsetDateTime.now());


        userTimelineMapper.insert(userTimelineEntity);

        // 测试 1：查询数据库
        List<UserTimelineEntity> userTimelineEntities = userTimelineMapper.selectAll();
        System.out.println(JSONUtil.toJsonPrettyStr(userTimelineEntities));
    }

    @Autowired
    UserPostReadMapper userPostReadMapper;

    @Rollback(false)
    @Test
    public void t12() {
        UserPostReadEntity userPostReadEntity = new UserPostReadEntity();
        userPostReadEntity.setUserId(0L);
        userPostReadEntity.setPostId(0L);
        userPostReadEntity.setCreatedAt(OffsetDateTime.now());
        userPostReadMapper.insert(userPostReadEntity);

        // 测试 1：查询数据库
        List<UserPostReadEntity> userPostReadEntityList = userPostReadMapper.selectAll();
        System.out.println(JSONUtil.toJsonPrettyStr(userPostReadEntityList));
    }

    @Autowired
    PostLinkCardMapper postLinkCardMapper;

    @Rollback(false)
    @Test
    public void t13() {
        PostLinkCardEntity postLinkCardEntity = new PostLinkCardEntity();
        postLinkCardEntity.setPostId(0L);
        postLinkCardEntity.setCardType("embed_video");
        postLinkCardEntity.setTargetUrl("");
        postLinkCardEntity.setVideoProvider("");
        postLinkCardEntity.setVideoId("");
        postLinkCardEntity.setCoverMediaUrl("");
        postLinkCardEntity.setCreatedAt(OffsetDateTime.now());

        postLinkCardMapper.insert(postLinkCardEntity);

        // 测试 1：查询数据库
        List<PostLinkCardEntity> postLinkCardEntityList = postLinkCardMapper.selectAll();
        System.out.println(JSONUtil.toJsonPrettyStr(postLinkCardEntityList));
    }
}