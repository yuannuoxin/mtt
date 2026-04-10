package com.mtd.common.core.constant;

public interface RequestParam {
    /**
     * 单 ID 查询参数类：{@link com.mtd.common.core.request.IdRequest}
     * 适用场景：根据单个 ID 查询、删除等操作
     */
    String ID_REQUEST = "IdRequest";

    /**
     * 列表查询参数类（不分页）：{@link com.mtd.common.core.request.ListRequest}
     * 适用场景：查询所有数据，不带分页功能
     */
    String LIST_REQUEST = "ListRequest";

    /**
     * 分页查询参数类：{@link com.mtd.common.core.request.PageRequest}
     * 适用场景：需要分页的列表查询
     */
    String PAGE_REQUEST = "PageRequest";
}
