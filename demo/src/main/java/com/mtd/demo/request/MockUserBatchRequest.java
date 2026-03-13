package com.mtd.demo.request;

import lombok.Data;

/**
 * 批量创建模拟用户请求参数
 */
@Data
public class MockUserBatchRequest {

    /**
     * 生成数量（默认 10 个）
     */
    private Integer count = 10;
}
