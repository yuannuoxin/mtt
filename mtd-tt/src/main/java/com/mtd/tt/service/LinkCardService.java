package com.mtd.tt.service;

import cn.hutool.core.util.StrUtil;
import com.mtd.tt.enums.CardTypeEnum;
import com.mtd.tt.enums.VideoProviderEnum;
import com.mtd.tt.model.entity.PostLinkCardEntity;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 链接卡片解析：抓取外部链接页面的 OG 元信息（标题/描述/封面），识别第三方视频（bilibili/youtube）。
 *
 * <p>发推时正文包含链接时自动调用，生成 link / embed_video 卡片。
 * 抓取失败、页面无 OG 信息时返回仅含 targetUrl 的最小卡片，不阻塞发帖。
 */
@Slf4j
@Service
public class LinkCardService {

    /** 正文中提取第一个 http(s) 链接 */
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s，。；！？、（）\"'<>【】]+");

    /** bilibili：www.bilibili.com/video/BVxxx 或 b23.tv/BVxxx */
    private static final Pattern BILI_VIDEO = Pattern.compile("bilibili\\.com/video/(BV[0-9A-Za-z]+)|b23\\.tv/(BV[0-9A-Za-z]+)", Pattern.CASE_INSENSITIVE);
    /** youtube：youtube.com/watch?v= 或 youtu.be/ */
    private static final Pattern YT_VIDEO = Pattern.compile("(?:youtube\\.com/(?:watch\\?.*v=|embed/|shorts/)|youtu\\.be/)([0-9A-Za-z_-]{11})", Pattern.CASE_INSENSITIVE);

    /** 从正文提取第一个 http(s) 链接，无则返回 null */
    public String extractUrl(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        Matcher m = URL_PATTERN.matcher(content);
        return m.find() ? m.group().trim() : null;
    }

    /**
     * 解析一个外部链接，生成链接卡片实体（postId 由调用方填充）。
     * 抓取失败或页面无法解析时，返回仅含 targetUrl 的 link 最小卡片。
     */
    public PostLinkCardEntity resolve(String url) {
        PostLinkCardEntity card = new PostLinkCardEntity();
        card.setCardType(CardTypeEnum.LINK.getCode());
        card.setTargetUrl(url);

        // 先识别第三方视频（bilibili/youtube）：纯 URL 解析，不依赖网络抓取
        String[] videoHit = detectVideo(url);
        if (videoHit != null) {
            card.setCardType(CardTypeEnum.EMBED_VIDEO.getCode());
            card.setVideoProvider(videoHit[0]);
            card.setVideoId(videoHit[1]);
            String cover = fetchOgImage(url);
            if (StrUtil.isBlank(cover) && VideoProviderEnum.YOUTUBE.getCode().equals(videoHit[0])) {
                cover = "https://img.youtube.com/vi/" + videoHit[1] + "/hqdefault.jpg";
            }
            card.setCoverMediaUrl(cover);
            return card;
        }

        try {
            Document doc = fetchDoc(url);
            if (doc != null) {
                String title = og(doc, "og:title");
                String description = og(doc, "og:description");
                String image = og(doc, "og:image");
                if (StrUtil.isNotBlank(title)) {
                    card.setTitle(truncate(title, 255));
                }
                if (StrUtil.isNotBlank(description)) {
                    card.setDescription(truncate(description, 1000));
                }
                if (StrUtil.isNotBlank(image)) {
                    card.setCoverMediaUrl(image);
                }
            }
        } catch (Exception e) {
            log.debug("链接卡片解析失败 url={}: {}", url, e.getMessage());
        }
        return card;
    }

    /** 仅抓取页面 og:image（用于视频卡片封面），失败返回 null */
    private String fetchOgImage(String url) {
        try {
            Document doc = fetchDoc(url);
            return doc == null ? null : og(doc, "og:image");
        } catch (Exception e) {
            log.debug("封面抓取失败 url={}: {}", url, e.getMessage());
            return null;
        }
    }

    private Document fetchDoc(String url) throws java.io.IOException {
        return Jsoup.connect(url)
                .timeout(5000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .followRedirects(true)
                .get();
    }

    /** 从 JSoup 文档读 og meta（缺省回退到页面 <title>） */
    private String og(Document doc, String property) {
        Element meta = doc.selectFirst("meta[property=" + property + "]");
        if (meta == null) {
            meta = doc.selectFirst("meta[name=" + property + "]");
        }
        String v = meta == null ? null : meta.attr("content");
        if (StrUtil.isBlank(v)) {
            v = doc.title();
        }
        return StrUtil.trimToNull(v);
    }

    /** 识别 bilibili / youtube 视频；命中返回 [provider, videoId]，否则 null */
    private String[] detectVideo(String url) {
        Matcher bili = BILI_VIDEO.matcher(url);
        if (bili.find()) {
            String bv = bili.group(1) != null ? bili.group(1) : bili.group(2);
            return new String[]{VideoProviderEnum.BILIBILI.getCode(), bv};
        }
        Matcher yt = YT_VIDEO.matcher(url);
        if (yt.find()) {
            return new String[]{VideoProviderEnum.YOUTUBE.getCode(), yt.group(1)};
        }
        return null;
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }
}
