package com.mtd.tt.model.entity;


import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("post_link_cards")
@FieldNameConstants
public class PostLinkCardEntity extends BaseEntity {
    private Long postId;
    private String cardType;
    private String targetUrl;
    private String videoProvider;
    private String videoId;
    private String coverMediaUrl;

    private OffsetDateTime createdAt;
}
