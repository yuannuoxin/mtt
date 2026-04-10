package com.mtd.common.core.constant;

public interface ApiType {
    /**
     * 单条数据查询 - 返回单个对象
     * 示例：/get
     */
    String GET = "get";

    /**
     * 列表查询（不分页） - 返回所有符合条件的数据
     * 示例：/selectList
     */
    String SELECT_LIST = "selectList";

    /**
     * 分页查询 - 返回分页数据
     * 示例：/selectPage
     */
    String SELECT_PAGE = "selectPage";

    /**
     * 保存/创建 - 新增数据
     * 示例：/save
     */
    String SAVE = "save";

    /**
     * 更新 - 修改数据
     * 示例：/update
     */
    String UPDATE = "update";

    /**
     * 删除 - 删除数据
     * 示例：/delete
     */
    String DELETE = "delete";
}
