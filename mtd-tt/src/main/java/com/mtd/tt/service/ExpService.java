package com.mtd.tt.service;

import com.mtd.common.core.request.PageRequest;
import com.mtd.common.core.response.PageResponse;
import com.mtd.tt.enums.ExpTypeEnum;
import com.mtd.tt.enums.PostTypeEnum;
import com.mtd.tt.mapper.ExpRecordMapper;
import com.mtd.tt.model.entity.ExpRecordEntity;
import com.mtd.tt.model.resp.ExpRecordResp;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 经验等级：发帖/转发/引用/回复按类型获得经验（发帖最多，其次引用/转发/回复），
 * 经验达到配置的挡位后自动提升积分等级（users.level）。
 * 规则通过 sys_config 可配：
 *  - exp.per.post / exp.per.quote / exp.per.repost / exp.per.reply 各类型获得经验
 *  - exp.level.thresholds  等级挡位（逗号分隔，升到 2/3/4/... 级所需累计经验）
 * 经验变动落 exp_records 表，用户可在经验页查看记录与等级关系。
 */
@Slf4j
@Service
public class ExpService {

    private static final String CFG_PER_POST = "exp.per.post";
    private static final String CFG_PER_QUOTE = "exp.per.quote";
    private static final String CFG_PER_REPOST = "exp.per.repost";
    private static final String CFG_PER_REPLY = "exp.per.reply";
    private static final String CFG_LEVEL_THRESHOLDS = "exp.level.thresholds";

    @Autowired
    private SysConfigService configService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ExpRecordMapper recordMapper;

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS exp_records
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
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_exp_records_user ON exp_records (user_id, created_at DESC)");
            log.info("经验记录表初始化完成");
        } catch (Exception e) {
            log.error("经验记录表初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发帖类动作获得经验：发帖最多，引用/转发/回复相对较少。
     */
    public void gainExp(Long userId, String postType) {
        int gain = gainOf(postType);
        if (gain <= 0 || userId == null) {
            return;
        }
        Long exp = jdbcTemplate.queryForObject("SELECT exp FROM users WHERE id = ?", Long.class, userId);
        long newExp = (exp == null ? 0L : exp) + gain;
        int level = computeLevel(newExp);
        jdbcTemplate.update("UPDATE users SET exp = ?, level = ? WHERE id = ?", newExp, level, userId);
        insertRecord(userId, postType, gain, newExp);
    }

    /**
     * 分页查询当前用户经验记录
     */
    public PageResponse<ExpRecordResp> records(Long userId, PageRequest page) {
        com.mybatisflex.core.paginate.Page<ExpRecordEntity> p =
                recordMapper.paginate(page.getPageNum(), page.getPageSize(),
                        com.mybatisflex.core.query.QueryWrapper.create()
                                .eq(ExpRecordEntity::getUserId, userId)
                                .orderBy(ExpRecordEntity::getCreatedAt, false)
                                .orderBy(ExpRecordEntity::getId, false));
        List<ExpRecordResp> list = p.getRecords().stream().map(this::toResp).toList();
        return PageResponse.of((int) p.getPageNumber(), (int) p.getPageSize(), p.getTotalRow(), list);
    }

    /**
     * 等级挡位：从 exp.level.thresholds 解析，返回 [{level, minExp, label}]，
     * 前端用于展示"经验与等级关系"。如 thresholds=100,300,600 → Lv2:100 / Lv3:300 / Lv4:600。
     */
    public List<Map<String, Object>> levelRules() {
        List<Map<String, Object>> rules = new ArrayList<>();
        rules.add(Map.of("level", 1, "minExp", 0L, "label", "Lv1"));
        String thresholds = configService.get(CFG_LEVEL_THRESHOLDS);
        if (thresholds == null || thresholds.isBlank()) {
            return rules;
        }
        int level = 1;
        for (String t : thresholds.split(",")) {
            String trimmed = t.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                long minExp = Long.parseLong(trimmed);
                level++;
                rules.add(Map.of("level", level, "minExp", minExp, "label", "Lv" + level));
            } catch (NumberFormatException ignored) {
            }
        }
        return rules;
    }

    private ExpRecordResp toResp(ExpRecordEntity e) {
        ExpRecordResp r = new ExpRecordResp();
        r.setId(e.getId());
        r.setType(e.getType());
        r.setPoints(e.getPoints());
        r.setBalance(e.getBalance());
        r.setRemark(e.getRemark());
        r.setCreatedAt(e.getCreatedAt());
        ExpTypeEnum t = ExpTypeEnum.of(e.getType());
        r.setTypeDesc(t == null ? e.getType() : t.getDesc());
        return r;
    }

    private void insertRecord(Long userId, String postType, long gain, long balance) {
        ExpRecordEntity rec = new ExpRecordEntity();
        rec.setUserId(userId);
        rec.setType(postType);
        rec.setPoints(gain);
        rec.setBalance(balance);
        rec.setRemark(expRemark(postType));
        rec.setCreatedAt(OffsetDateTime.now());
        recordMapper.insertSelective(rec);
    }

    private String expRemark(String postType) {
        if (PostTypeEnum.REPLY.getCode().equals(postType)) {
            return "发布回复获得经验";
        }
        if (PostTypeEnum.QUOTE.getCode().equals(postType)) {
            return "发布引用获得经验";
        }
        if (PostTypeEnum.REPOST.getCode().equals(postType)) {
            return "发布转发获得经验";
        }
        return "发布帖子获得经验";
    }

    private int gainOf(String postType) {
        if (PostTypeEnum.REPLY.getCode().equals(postType)) {
            return configService.getInt(CFG_PER_REPLY, 1);
        }
        if (PostTypeEnum.QUOTE.getCode().equals(postType)) {
            return configService.getInt(CFG_PER_QUOTE, 3);
        }
        if (PostTypeEnum.REPOST.getCode().equals(postType)) {
            return configService.getInt(CFG_PER_REPOST, 2);
        }
        return configService.getInt(CFG_PER_POST, 5);
    }

    /**
     * 根据累计经验计算等级：每超过一个阈值挡位升一级（默认 Lv1）。
     */
    private int computeLevel(long exp) {
        String thresholds = configService.get(CFG_LEVEL_THRESHOLDS);
        if (thresholds == null || thresholds.isBlank()) {
            return 1;
        }
        int level = 1;
        for (String t : thresholds.split(",")) {
            String trimmed = t.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                if (exp >= Long.parseLong(trimmed)) {
                    level++;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return level;
    }
}
