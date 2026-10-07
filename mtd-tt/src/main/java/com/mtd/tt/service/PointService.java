package com.mtd.tt.service;

import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.request.PageRequest;
import com.mtd.common.core.response.PageResponse;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.enums.PointTypeEnum;
import com.mtd.tt.enums.PostTypeEnum;
import com.mtd.tt.mapper.PointRecordMapper;
import com.mtd.tt.model.entity.PointRecordEntity;
import com.mtd.tt.model.resp.CheckinResp;
import com.mtd.tt.model.resp.PointRecordResp;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 积分系统：每日签到送积分、发帖/转发/引用/回复/上传文件扣积分，并记录每次变动。
 * 积分规则通过 sys_config 可配：
 *  - points.checkin          签到送几分（默认 1）
 *  - points.post             发帖/转发/引用 扣几分（默认 10）
 *  - points.reply            回复扣几分（默认 2）
 *  - points.upload_per_mb    上传每 1MB 扣几分（不足 1MB 按 1MB，默认 1）
 *  - points.initial          新用户注册初始积分（默认 100）
 * 扣分采用行级原子 UPDATE（points >= 需扣分）保证并发不超扣；余额不足则拒绝。
 */
@Slf4j
@Service
public class PointService {

    private static final String CFG_CHECKIN = "points.checkin";
    private static final String CFG_POST = "points.post";
    private static final String CFG_REPLY = "points.reply";
    private static final String CFG_UPLOAD_PER_MB = "points.upload_per_mb";
    private static final String CFG_INITIAL = "points.initial";

    private static final long MB = 1024L * 1024L;

    @Autowired
    private PointRecordMapper recordMapper;
    @Autowired
    private SysConfigService configService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS points BIGINT NOT NULL DEFAULT 0");
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS point_records
                    (
                        id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        user_id    BIGINT      NOT NULL,
                        type       VARCHAR(30) NOT NULL,
                        points     BIGINT      NOT NULL,
                        balance    BIGINT      NOT NULL,
                        remark     VARCHAR(255),
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                    )
                    """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_point_records_user ON point_records (user_id, created_at DESC)");
            log.info("积分表初始化完成");
        } catch (Exception e) {
            log.error("积分表初始化失败: {}", e.getMessage(), e);
        }
    }

    public int initialPoint() {
        return configService.getInt(CFG_INITIAL, 100);
    }

    public int checkinReward() {
        return configService.getInt(CFG_CHECKIN, 1);
    }

    public int postCost() {
        return configService.getInt(CFG_POST, 10);
    }

    public int replyCost() {
        return configService.getInt(CFG_REPLY, 2);
    }

    public int uploadPerMb() {
        return configService.getInt(CFG_UPLOAD_PER_MB, 1);
    }

    /**
     * 注册赠送初始积分（users.points 已在插入时设置，这里仅落一条 register 记录）
     */
    public void grantRegisterPoints(Long userId) {
        int initial = initialPoint();
        insertRecord(userId, PointTypeEnum.REGISTER.getCode(), initial, initial, "注册赠送");
    }

    /**
     * 每日签到：当日已签则拒绝，否则送 checkinReward 积分。
     */
    @Transactional
    public CheckinResp checkin(Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        Long count = recordMapper.selectCountByQuery(
                com.mybatisflex.core.query.QueryWrapper.create()
                        .eq(PointRecordEntity::getUserId, userId)
                        .eq(PointRecordEntity::getType, PointTypeEnum.CHECKIN.getCode())
                        .ge(PointRecordEntity::getCreatedAt, todayStart));
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "今日已签到，明天再来吧");
        }
        int reward = checkinReward();
        long balance = addPoints(userId, reward, PointTypeEnum.CHECKIN.getCode(), "每日签到");
        CheckinResp resp = new CheckinResp();
        resp.setReward(reward);
        resp.setBalance(balance);
        resp.setCheckedIn(true);
        resp.setType(PointTypeEnum.CHECKIN.getCode());
        return resp;
    }

    /**
     * 发帖/转发/引用 扣 postCost；回复 扣 replyCost。
     */
    public void deductForPost(Long userId, String postType) {
        int cost;
        String type;
        String desc;
        if (PostTypeEnum.REPLY.getCode().equals(postType)) {
            cost = replyCost();
            type = PointTypeEnum.REPLY.getCode();
            desc = "发布回复";
        } else {
            cost = postCost();
            if (PostTypeEnum.QUOTE.getCode().equals(postType)) {
                type = PointTypeEnum.QUOTE.getCode();
                desc = "发布引用";
            } else if (PostTypeEnum.REPOST.getCode().equals(postType)) {
                type = PointTypeEnum.REPOST.getCode();
                desc = "发布转发";
            } else {
                type = PointTypeEnum.POST.getCode();
                desc = "发布帖子";
            }
        }
        deduct(userId, cost, type, desc);
    }

    /**
     * 上传文件扣积分：按文件大小向上取整到 MB，每 MB 扣 uploadPerMb 分；不足 1MB 按 1MB。
     */
    public void deductForUpload(Long userId, long sizeBytes) {
        if (sizeBytes < 0) {
            sizeBytes = 0;
        }
        long mb = Math.max(1, (sizeBytes + MB - 1) / MB);
        long cost = mb * uploadPerMb();
        deduct(userId, cost, PointTypeEnum.UPLOAD.getCode(), "上传文件 " + mb + "MB");
    }

    /**
     * 查询当前用户积分余额
     */
    public Long balanceOf(Long userId) {
        Long v = jdbcTemplate.queryForObject(
                "SELECT points FROM users WHERE id = ?", Long.class, userId);
        return v == null ? 0L : v;
    }

    /**
     * 分页查询积分记录
     */
    public PageResponse<PointRecordResp> records(Long userId, PageRequest page) {
        com.mybatisflex.core.paginate.Page<PointRecordEntity> p =
                recordMapper.paginate(page.getPageNum(), page.getPageSize(),
                        com.mybatisflex.core.query.QueryWrapper.create()
                                .eq(PointRecordEntity::getUserId, userId)
                                .orderBy(PointRecordEntity::getCreatedAt, false)
                                .orderBy(PointRecordEntity::getId, false));
        List<PointRecordResp> list = p.getRecords().stream().map(this::toResp).toList();
        return PageResponse.of((int) p.getPageNumber(), (int) p.getPageSize(), p.getTotalRow(), list);
    }

    private PointRecordResp toResp(PointRecordEntity e) {
        PointRecordResp r = new PointRecordResp();
        r.setId(e.getId());
        r.setType(e.getType());
        r.setPoints(e.getPoints());
        r.setBalance(e.getBalance());
        r.setRemark(e.getRemark());
        r.setCreatedAt(e.getCreatedAt());
        PointTypeEnum typeEnum = PointTypeEnum.of(e.getType());
        r.setTypeDesc(typeEnum == null ? e.getType() : typeEnum.getDesc());
        return r;
    }

    /**
     * 扣分：行级原子更新，余额不足抛"积分不足"。
     */
    private void deduct(Long userId, long cost, String type, String remark) {
        if (cost <= 0) {
            return;
        }
        int updated = jdbcTemplate.update(
                "UPDATE users SET points = points - ? WHERE id = ? AND points >= ?",
                cost, userId, cost);
        if (updated == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "积分不足，无法完成该操作");
        }
        long balance = balanceOf(userId);
        insertRecord(userId, type, -cost, balance, remark);
    }

    /**
     * 加分（签到/注册赠送等）：行级原子累加。
     */
    private long addPoints(Long userId, long reward, String type, String remark) {
        jdbcTemplate.update("UPDATE users SET points = points + ? WHERE id = ?", reward, userId);
        long balance = balanceOf(userId);
        insertRecord(userId, type, reward, balance, remark);
        return balance;
    }

    private void insertRecord(Long userId, String type, long points, long balance, String remark) {
        PointRecordEntity rec = new PointRecordEntity();
        rec.setUserId(userId);
        rec.setType(type);
        rec.setPoints(points);
        rec.setBalance(balance);
        rec.setRemark(remark);
        rec.setCreatedAt(OffsetDateTime.now());
        recordMapper.insertSelective(rec);
    }
}
