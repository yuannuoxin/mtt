package com.mtd.tt.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正文隐藏块语法：[hide login] / [hide reply] / [hide points=N] / [hide password=xxx]
 * 以 [/hide] 闭合。解析与脱敏规则与 Go 版 internal/markdown/hide.go 保持一致。
 */
public final class HideMarkdown {

    public static final String KIND_LOGIN = "login";
    public static final String KIND_REPLY = "reply";
    public static final String KIND_POINTS = "points";
    public static final String KIND_PASSWORD = "password";
    public static final int MAX_PASSWORD_LENGTH = 64;

    private static final Pattern OPEN_RE = Pattern.compile("(?i)^\\[hide(?:\\s+(.+))?\\]$");
    private static final Pattern PASSWORD_RE = Pattern.compile("(?i)(\\[hide\\s+password=)([^\\s\\]]+)([^\\]]*\\])");
    private static final Pattern FENCE_RE = Pattern.compile("(?s)```.*?```");
    private static final Pattern INLINE_CODE_RE = Pattern.compile("`[^`\\n]*`");

    private HideMarkdown() {
    }

    public static final class HideBlock {
        public final int index;
        public final String kind;
        public final int points;
        public final String password;
        public final String body;
        public final int start;
        public final int end;

        public HideBlock(int index, String kind, int points, String password, String body, int start, int end) {
            this.index = index;
            this.kind = kind;
            this.points = points;
            this.password = password;
            this.body = body;
            this.start = start;
            this.end = end;
        }
    }

    public static final class Status {
        public final int index;
        public final String kind;
        public final int points;
        public final boolean unlocked;

        public Status(int index, String kind, int points, boolean unlocked) {
            this.index = index;
            this.kind = kind;
            this.points = points;
            this.unlocked = unlocked;
        }
    }

    public static final class Sanitized {
        public final String content;
        public final List<Status> statuses;

        public Sanitized(String content, List<Status> statuses) {
            this.content = content;
            this.statuses = statuses;
        }
    }

    public static final class ViewerCaps {
        public final boolean bypass;
        public final boolean loggedIn;
        public final boolean replied;
        public final boolean paid;
        public final Set<Integer> passwordUnlocked;

        public ViewerCaps(boolean bypass, boolean loggedIn, boolean replied, boolean paid,
                          Set<Integer> passwordUnlocked) {
            this.bypass = bypass;
            this.loggedIn = loggedIn;
            this.replied = replied;
            this.paid = paid;
            this.passwordUnlocked = passwordUnlocked == null ? Collections.emptySet() : passwordUnlocked;
        }
    }

    /**
     * 扫描正文中的隐藏块；代码围栏内的伪语法不参与解析。
     * 语法错误抛 IllegalArgumentException，由调用方转成业务错误。
     */
    public static List<HideBlock> parse(String content) {
        List<String> lines = splitLines(content);
        List<HideBlock> blocks = new ArrayList<>();
        boolean inFence = false;

        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (isFence(trimmed)) {
                inFence = !inFence;
                continue;
            }
            if (inFence) {
                continue;
            }
            if (trimmed.equalsIgnoreCase("[/hide]")) {
                throw new IllegalArgumentException("隐藏块语法无效");
            }
            if (!looksLikeHideOpen(trimmed)) {
                continue;
            }

            Open open = parseOpen(trimmed);
            int j = i + 1;
            boolean innerFence = false;
            boolean closed = false;
            List<String> body = new ArrayList<>();
            for (; j < lines.size(); j++) {
                String inner = lines.get(j).trim();
                if (isFence(inner)) {
                    innerFence = !innerFence;
                    body.add(lines.get(j));
                    continue;
                }
                if (innerFence) {
                    body.add(lines.get(j));
                    continue;
                }
                if (inner.equalsIgnoreCase("[/hide]")) {
                    closed = true;
                    break;
                }
                if (looksLikeHideOpen(inner)) {
                    throw new IllegalArgumentException("隐藏块不能嵌套");
                }
                body.add(lines.get(j));
            }
            if (!closed) {
                throw new IllegalArgumentException("隐藏块未闭合");
            }

            blocks.add(new HideBlock(blocks.size(), open.kind, open.points, open.password,
                    String.join("\n", body), i, j));
            i = j;
        }
        return blocks;
    }

    /** 发布前校验：有隐藏意图就必须是合法隐藏块 */
    public static void validate(String content) {
        List<HideBlock> blocks = parse(content);
        if (blocks.isEmpty() && hasHideIntent(content)) {
            throw new IllegalArgumentException("隐藏块语法无效");
        }
    }

    /** 是否含隐藏意图（忽略代码围栏与行内代码） */
    public static boolean hasHideIntent(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        String plain = FENCE_RE.matcher(content).replaceAll("");
        plain = INLINE_CODE_RE.matcher(plain).replaceAll("");
        return plain.toLowerCase().contains("[hide");
    }

    public static int sumPoints(List<HideBlock> blocks) {
        int total = 0;
        for (HideBlock b : blocks) {
            if (KIND_POINTS.equals(b.kind)) {
                total += b.points;
            }
        }
        return total;
    }

    /**
     * 按读者能力脱敏：未解锁块的正文替换为等长空格，保证 @/# mention 下标不偏移；
     * 密码开标记始终打码，避免把明文密码下发。
     */
    public static Sanitized sanitize(String content, ViewerCaps caps) {
        List<HideBlock> blocks = parse(content);
        if (blocks.isEmpty()) {
            return new Sanitized(content, Collections.emptyList());
        }
        List<String> lines = splitLines(content);
        List<String> out = new ArrayList<>(lines);
        List<Status> statuses = new ArrayList<>(blocks.size());

        for (HideBlock block : blocks) {
            boolean unlocked = isUnlocked(block, caps);
            statuses.add(new Status(block.index, block.kind, block.points, unlocked));
            if (KIND_PASSWORD.equals(block.kind)) {
                out.set(block.start, maskPassword(out.get(block.start)));
            }
            if (!unlocked) {
                for (int i = block.start + 1; i < block.end; i++) {
                    out.set(i, blankLine(out.get(i)));
                }
            }
        }
        return new Sanitized(String.join("\n", out), statuses);
    }

    /** 返回密码匹配的隐藏块下标；无匹配抛“密码错误” */
    public static List<Integer> matchPassword(String content, String password) {
        String input = password == null ? "" : password.trim();
        if (input.isEmpty()) {
            throw new IllegalArgumentException("请输入密码");
        }
        List<HideBlock> blocks = parse(content);
        List<Integer> hits = new ArrayList<>();
        for (HideBlock block : blocks) {
            if (!KIND_PASSWORD.equals(block.kind)) {
                continue;
            }
            if (constantTimeEqual(block.password, input)) {
                hits.add(block.index);
            }
        }
        if (hits.isEmpty()) {
            throw new IllegalArgumentException("密码错误");
        }
        return hits;
    }

    private static boolean isUnlocked(HideBlock block, ViewerCaps caps) {
        if (caps.bypass) {
            return true;
        }
        return switch (block.kind) {
            case KIND_LOGIN -> caps.loggedIn;
            case KIND_REPLY -> caps.replied;
            case KIND_POINTS -> caps.paid;
            case KIND_PASSWORD -> caps.passwordUnlocked.contains(block.index);
            default -> true;
        };
    }

    private static final class Open {
        private final String kind;
        private final int points;
        private final String password;

        private Open(String kind, int points, String password) {
            this.kind = kind;
            this.points = points;
            this.password = password;
        }
    }

    private static Open parseOpen(String trimmed) {
        Matcher m = OPEN_RE.matcher(trimmed);
        if (!m.matches()) {
            throw new IllegalArgumentException("隐藏块语法无效");
        }
        String rest = m.group(1) == null ? "" : m.group(1).trim();
        if (rest.isEmpty()) {
            throw new IllegalArgumentException("隐藏块缺少可见条件");
        }
        String[] parts = rest.split("\\s+");
        String head = parts[0];
        String kind = head;
        String arg = "";
        int eq = head.indexOf('=');
        if (eq >= 0) {
            kind = head.substring(0, eq);
            arg = head.substring(eq + 1);
        }
        kind = kind.toLowerCase();
        if (!KIND_LOGIN.equals(kind) && !KIND_REPLY.equals(kind)
                && !KIND_POINTS.equals(kind) && !KIND_PASSWORD.equals(kind)) {
            throw new IllegalArgumentException("未知的隐藏条件: " + kind);
        }

        int points = 0;
        String password = "";
        if (KIND_POINTS.equals(kind)) {
            try {
                points = Integer.parseInt(arg);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("积分条件必须是数字");
            }
            if (points < 1 || points > 100000) {
                throw new IllegalArgumentException("积分需在 1-100000 之间");
            }
        } else if (KIND_PASSWORD.equals(kind)) {
            password = arg;
            if (password.isEmpty() || password.chars().anyMatch(Character::isWhitespace)) {
                throw new IllegalArgumentException("密码不能为空且不能包含空格");
            }
            if (password.codePointCount(0, password.length()) > MAX_PASSWORD_LENGTH) {
                throw new IllegalArgumentException("密码最多 64 个字符");
            }
        } else if (!arg.isEmpty()) {
            throw new IllegalArgumentException("隐藏条件不接受参数");
        }

        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].equalsIgnoreCase("locked")) {
                throw new IllegalArgumentException("隐藏块语法无效");
            }
        }
        return new Open(kind, points, password);
    }

    private static boolean looksLikeHideOpen(String line) {
        if (!line.regionMatches(true, 0, "[hide", 0, 5)) {
            return false;
        }
        if (line.length() <= 5) {
            return true;
        }
        char next = line.charAt(5);
        return Character.isWhitespace(next) || next == ']';
    }

    private static boolean isFence(String line) {
        return line.startsWith("```") || line.startsWith("~~~");
    }

    private static List<String> splitLines(String content) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        return List.of(normalized.split("\n", -1));
    }

    /** 用等 UTF-16 长度的空格覆盖隐藏正文 */
    private static String blankLine(String line) {
        return line.isEmpty() ? "" : " ".repeat(line.length());
    }

    private static String maskPassword(String line) {
        Matcher m = PASSWORD_RE.matcher(line);
        if (!m.find()) {
            return line;
        }
        StringBuilder sb = new StringBuilder(line.length());
        sb.append(line, 0, m.start(2));
        sb.append("*".repeat(m.group(2).length()));
        sb.append(line, m.end(2), line.length());
        return sb.toString();
    }

    private static boolean constantTimeEqual(String a, String b) {
        byte[] left = (a == null ? "" : a).getBytes(StandardCharsets.UTF_8);
        byte[] right = (b == null ? "" : b).getBytes(StandardCharsets.UTF_8);
        if (left.length != right.length) {
            return false;
        }
        return MessageDigest.isEqual(left, right);
    }
}
