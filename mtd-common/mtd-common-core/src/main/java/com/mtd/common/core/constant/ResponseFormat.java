package com.mtd.common.core.constant;

public interface ResponseFormat {
    /**
     * 单个对象响应
     * 格式：Result<T>
     */
    String SINGLE = "Result<T>";

    /**
     * 列表响应（不分页）
     * 格式：Result<List<T>>
     */
    String LIST = "Result<List<T>>";

    /**
     * 分页响应
     * 格式：Result<PageResponse<T>>
     */
    String PAGE = "Result<PageResponse<T>>";

    /**
     * 布尔值响应（操作成功/失败）
     * 格式：Result<Boolean>
     */
    String BOOLEAN = "Result<Boolean>";

    /**
     * 整数响应（数量等）
     * 格式：Result<Integer>
     */
    String INTEGER = "Result<Integer>";
}
