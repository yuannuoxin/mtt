package com.mtd.tt.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "删除媒体文件")
public class DeleteFileReq {
    @Schema(description = "文件记录 ID")
    private Long id;
}
