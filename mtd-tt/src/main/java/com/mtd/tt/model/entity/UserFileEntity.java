package com.mtd.tt.model.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

/**
 * 用户上传文件记录：发推后可回填 postId 关联到具体推文
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("user_files")
@FieldNameConstants
public class UserFileEntity extends BaseEntity {
    private Long userId;
    /** 场景 FileSceneEnum：avatar/banner/post */
    private String scene;
    /** 媒体类型：image/video/audio */
    private String mediaType;
    /** MinIO 对象 key（相对路径） */
    private String objectKey;
    /** 可访问 URL */
    private String url;
    private Long size;
    private String contentType;
    /** 归属推文 id，发推后回填 */
    private Long postId;
    /** 是否缩略图 */
    private Boolean thumbnail;
    /** 缩略图对应的原图 user_files.id（新上传显式写入；老数据为空，查询时按时间兜底） */
    private Long thumbnailOfFileId;
    private OffsetDateTime createdAt;
}
