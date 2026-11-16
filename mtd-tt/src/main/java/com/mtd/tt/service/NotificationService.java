package com.mtd.tt.service;

import cn.hutool.core.util.ObjectUtil;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.request.PageRequest;
import com.mtd.common.core.response.PageResponse;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.mapper.NotificationMapper;
import com.mtd.tt.mapper.UserMapper;
import com.mtd.tt.model.entity.NotificationEntity;
import com.mtd.tt.model.entity.UserEntity;
import com.mtd.tt.model.resp.NotificationResp;
import com.mtd.tt.model.resp.UserProfileResp;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知服务：点赞/评论/关注/转发/提及
 */
@Slf4j
@Service
public class NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserService userService;

    /**
     * 创建通知（内部使用）
     */
    public void create(Long receiverUserId, Long triggerUserId, String type, Long postId) {
        if (receiverUserId == null || triggerUserId == null) {
            return;
        }
        NotificationEntity entity = new NotificationEntity();
        entity.setReceiverUserId(receiverUserId);
        entity.setTriggerUserId(triggerUserId);
        entity.setType(type);
        entity.setPostId(postId);
        entity.setRead(false);
        notificationMapper.insertSelective(entity);
    }

    /**
     * 通知列表
     */
    public PageResponse<NotificationResp> list(Long currentUserId, PageRequest page) {
        if (currentUserId == null) {
            return PageResponse.of(page.getPageNum(), page.getPageSize(), 0L, List.of());
        }
        Page<NotificationEntity> p = notificationMapper.paginate(page.getPageNum(), page.getPageSize(),
                QueryWrapper.create()
                        .eq(NotificationEntity::getReceiverUserId, currentUserId)
                        .orderBy(NotificationEntity::getCreatedAt, false));
        List<NotificationResp> list = p.getRecords().stream()
                .map(this::toResp)
                .filter(ObjectUtil::isNotNull)
                .collect(Collectors.toList());
        return PageResponse.of((int) p.getPageNumber(), (int) p.getPageSize(), p.getTotalRow(), list);
    }

    /**
     * 全部标记已读
     */
    public void markAllRead(Long currentUserId) {
        if (currentUserId == null) {
            return;
        }
        List<NotificationEntity> unread = notificationMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq(NotificationEntity::getReceiverUserId, currentUserId)
                        .eq(NotificationEntity::getRead, false));
        for (NotificationEntity entity : unread) {
            entity.setRead(true);
            notificationMapper.update(entity);
        }
    }

    /**
     * 单条标记已读（点击通知时调用）：只允许操作接收者本人的通知
     */
    public void markRead(Long currentUserId, Long id) {
        if (currentUserId == null || id == null || id <= 0) {
            return;
        }
        NotificationEntity entity = notificationMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq(NotificationEntity::getId, id)
                        .eq(NotificationEntity::getReceiverUserId, currentUserId)
                        .eq(NotificationEntity::getRead, false));
        if (entity == null) {
            return;
        }
        entity.setRead(true);
        notificationMapper.update(entity);
    }

    /**
     * 未读通知数量
     */
    public long unreadCount(Long currentUserId) {
        if (currentUserId == null) {
            return 0;
        }
        Long cnt = notificationMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq(NotificationEntity::getReceiverUserId, currentUserId)
                        .eq(NotificationEntity::getRead, false));
        return cnt == null ? 0 : cnt;
    }

    private NotificationResp toResp(NotificationEntity entity) {
        NotificationResp resp = new NotificationResp();
        resp.setId(entity.getId());
        resp.setType(entity.getType());
        resp.setTriggerUserId(entity.getTriggerUserId());
        resp.setPostId(entity.getPostId());
        resp.setRead(entity.getRead());
        resp.setCreatedAt(entity.getCreatedAt());

        UserEntity trigger = userMapper.selectOneById(entity.getTriggerUserId());
        if (trigger != null) {
            UserProfileResp triggerUser = userService.buildProfile(trigger, entity.getReceiverUserId(), false);
            resp.setTriggerUser(triggerUser);
        }
        return resp;
    }
}
