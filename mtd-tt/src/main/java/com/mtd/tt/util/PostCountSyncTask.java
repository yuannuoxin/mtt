package com.mtd.tt.util;

import cn.hutool.core.collection.CollUtil;
import com.mtd.tt.mapper.PostMapper;
import com.mtd.tt.model.entity.PostEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 每日定时任务：把 Redis 中的帖子计数（like/reply/repost/view/bookmark）写回数据库 posts 表。
 *
 * <p>背景：计数平时只维护在 Redis（INCR/DECR），为防 Redis 数据丢失后计数回退，
 * 每天凌晨人少时做一次落库同步，保证 DB 与 Redis 最终一致。
 * 关系数据（点赞/收藏记录）本就实时写库，无需在此处理。
 */
@Slf4j
@Component
public class PostCountSyncTask {

    /** 计数 key 前缀：tt:post:{postId}:cnt:{action} */
    private static final String CNT_KEY_PREFIX = "tt:post:";
    private static final String CNT_KEY_SUFFIX = ":cnt:";

    private final StringRedisTemplate redis;
    private final PostMapper postMapper;

    public PostCountSyncTask(StringRedisTemplate redis, PostMapper postMapper) {
        this.redis = redis;
        this.postMapper = postMapper;
    }

    /** 每天凌晨 03:00 执行一次（避开使用高峰） */
    @Scheduled(cron = "0 0 3 * * *")
    public void syncCounts() {
        Set<String> keys;
        try {
            // 小规模项目用 keys 扫描即可；数据量大时可换 SCAN 迭代
            keys = redis.keys(CNT_KEY_PREFIX + "*" + CNT_KEY_SUFFIX + "*");
        } catch (Exception e) {
            log.warn("Redis 计数同步失败（读取 keys 异常）：{}", e.getMessage());
            return;
        }
        if (CollUtil.isEmpty(keys)) {
            log.info("Redis 计数同步：无计数 key，跳过");
            return;
        }

        // 按 postId 聚合 5 类计数，同一帖只 UPDATE 一次
        Map<Long, PostEntity> agg = new HashMap<>();
        for (String key : keys) {
            // tt:post:{postId}:cnt:{action}
            String[] parts = key.split(":");
            if (parts.length < 5) {
                continue;
            }
            Long postId;
            try {
                postId = Long.parseLong(parts[2]);
            } catch (NumberFormatException e) {
                continue;
            }
            String action = parts[4];
            String v = redis.opsForValue().get(key);
            if (v == null) {
                continue;
            }
            long val;
            try {
                val = Long.parseLong(v);
            } catch (NumberFormatException e) {
                continue;
            }
            PostEntity e = agg.computeIfAbsent(postId, k -> {
                PostEntity pe = new PostEntity();
                pe.setId(postId);
                return pe;
            });
            switch (action) {
                case RedisPostCounter.LIKE -> e.setLikeCount(val);
                case RedisPostCounter.BOOKMARK -> e.setBookmarkCount(val);
                case RedisPostCounter.REPOST -> e.setRepostCount(val);
                case RedisPostCounter.REPLY -> e.setReplyCount(val);
                case RedisPostCounter.VIEW -> e.setViewCount(val);
                default -> {
                }
            }
        }

        if (agg.isEmpty()) {
            log.info("Redis 计数同步：无可更新数据");
            return;
        }
        int updated = 0;
        for (PostEntity e : agg.values()) {
            try {
                postMapper.update(e, true); // 忽略 null，只更新已聚合的计数字段
                updated++;
            } catch (Exception ex) {
                log.warn("Redis 计数同步：帖子 {} 更新失败：{}", e.getId(), ex.getMessage());
            }
        }
        log.info("Redis 计数同步完成，更新帖子数={}", updated);

        syncHotScores();
    }

    /**
     * 把 Redis 中未达挡位而残留的热度增量兜底落库（每天凌晨执行一次），保证热度最终一致。
     */
    private void syncHotScores() {
        Set<String> hotKeys;
        try {
            hotKeys = redis.keys("tt:post:*:hot");
        } catch (Exception e) {
            log.warn("热度同步失败（读取 keys 异常）：{}", e.getMessage());
            return;
        }
        if (CollUtil.isEmpty(hotKeys)) {
            return;
        }
        int updated = 0;
        for (String key : hotKeys) {
            // tt:post:{postId}:hot
            String[] parts = key.split(":");
            if (parts.length < 4) {
                continue;
            }
            Long postId;
            try {
                postId = Long.parseLong(parts[2]);
            } catch (NumberFormatException e) {
                continue;
            }
            String raw = redis.opsForValue().getAndDelete(key);
            if (raw == null) {
                continue;
            }
            Long delta;
            try {
                delta = Long.parseLong(raw);
            } catch (NumberFormatException e) {
                continue;
            }
            if (delta == 0) {
                continue;
            }
            try {
                postMapper.updateHotScore(postId, delta);
                updated++;
            } catch (Exception ex) {
                log.warn("热度同步：帖子 {} 更新失败：{}", postId, ex.getMessage());
            }
        }
        log.info("热度同步完成，更新帖子数={}", updated);
    }
}
