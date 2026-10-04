package com.mtd.tt.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 帖子的点赞/收藏/转发/回复/浏览 计数与关系缓存（Redis）。
 *
 * <p>目的：计数与"当前用户是否点赞/收藏/转发"的判断不再每次读写数据库表，
 * 改为 Redis 维护，减少数据库压力。DB 的关系表与 posts 计数字段保留作为
 * 持久层与冷启动兜底（关系表仍写库，保证数据可靠与"我的收藏"可用）。
 *
 * <p>Key 约定：
 * <ul>
 *   <li>计数：{@code tt:post:{postId}:cnt:{action}}（String，INCR/DECR）</li>
 *   <li>关系：{@code tt:post:{postId}:users:{action}}（Set，存 userId）</li>
 * </ul>
 */
@Component
public class RedisPostCounter {

    /** 点赞 */
    public static final String LIKE = "like";
    /** 收藏 */
    public static final String BOOKMARK = "bookmark";
    /** 转发 */
    public static final String REPOST = "repost";
    /** 回复 */
    public static final String REPLY = "reply";
    /** 浏览 */
    public static final String VIEW = "view";

    private static final List<String> ACTIONS = List.of(LIKE, BOOKMARK, REPOST, REPLY, VIEW);

    private final StringRedisTemplate redis;

    public RedisPostCounter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String cntKey(Long postId, String action) {
        return "tt:post:" + postId + ":cnt:" + action;
    }

    private String setKey(Long postId, String action) {
        return "tt:post:" + postId + ":users:" + action;
    }

    /** 计数 +1，返回最新值 */
    public long incr(Long postId, String action) {
        Long v = redis.opsForValue().increment(cntKey(postId, action));
        return v == null ? 0 : v;
    }

    /** 计数 -1（不低于 0），返回最新值 */
    public long decr(Long postId, String action) {
        Long v = redis.opsForValue().decrement(cntKey(postId, action));
        return v == null ? 0 : Math.max(0, v);
    }

    /** 记录某用户对该帖的交互（关系集合 + 计数 +1） */
    public void addUser(Long postId, String action, Long userId) {
        if (userId == null) {
            return;
        }
        redis.opsForSet().add(setKey(postId, action), String.valueOf(userId));
        incr(postId, action);
    }

    /** 取消某用户对该帖的交互（关系集合移除 + 计数 -1） */
    public void removeUser(Long postId, String action, Long userId) {
        if (userId == null) {
            return;
        }
        redis.opsForSet().remove(setKey(postId, action), String.valueOf(userId));
        decr(postId, action);
    }

    /** 当前用户是否已点赞/收藏/转发（Redis 集合判断，不查库） */
    public boolean hasUser(Long postId, String action, Long userId) {
        if (userId == null) {
            return false;
        }
        return Boolean.TRUE.equals(redis.opsForSet().isMember(setKey(postId, action), String.valueOf(userId)));
    }

    /** 读取计数：Redis 有则返回；无则用 fallback（DB 字段）兜底并回填 Redis */
    public long getCount(Long postId, String action, long fallback) {
        String key = cntKey(postId, action);
        String v = redis.opsForValue().get(key);
        if (v == null) {
            redis.opsForValue().set(key, String.valueOf(fallback));
            return fallback;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
