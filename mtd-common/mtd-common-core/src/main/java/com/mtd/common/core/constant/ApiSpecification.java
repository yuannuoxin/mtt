package com.mtd.common.core.constant;

/**
 * 接口设计规范常量
 * 用于定义项目中接口的命名和参数规范
 */
public class ApiSpecification {

    /**
     * 接口类型定义
     */
    public static class ApiType {
        /**
         * 单条数据查询 - 返回单个对象
         * 示例：/user/get
         */
        public static final String GET = "get";

        /**
         * 列表查询（不分页） - 返回所有符合条件的数据
         * 示例：/user/list/all
         */
        public static final String LIST_ALL = "list/all";

        /**
         * 分页查询 - 返回分页数据
         * 示例：/user/list/page
         */
        public static final String LIST_PAGE = "list/page";

        /**
         * 保存/创建 - 新增数据
         * 示例：/user/save
         */
        public static final String SAVE = "save";

        /**
         * 更新 - 修改数据
         * 示例：/user/update
         */
        public static final String UPDATE = "update";

        /**
         * 删除 - 删除数据
         * 示例：/user/delete
         */
        public static final String DELETE = "delete";
    }

    /**
     * 请求参数规范
     */
    public static class RequestParam {
        /**
         * 单 ID 查询参数类：{@link com.mtd.common.core.request.IdRequest}
         * 适用场景：根据单个 ID 查询、删除等操作
         */
        public static final String ID_REQUEST = "IdRequest";

        /**
         * 列表查询参数类（不分页）：{@link com.mtd.common.core.request.ListRequest}
         * 适用场景：查询所有数据，不带分页功能
         */
        public static final String LIST_REQUEST = "ListRequest";

        /**
         * 分页查询参数类：{@link com.mtd.common.core.request.PageRequest}
         * 适用场景：需要分页的列表查询
         */
        public static final String PAGE_REQUEST = "PageRequest";
    }

    /**
     * 响应结果规范
     */
    public static class ResponseFormat {
        /**
         * 单个对象响应
         * 格式：Result<T>
         */
        public static final String SINGLE = "Result<T>";

        /**
         * 列表响应（不分页）
         * 格式：Result<List<T>>
         */
        public static final String LIST = "Result<List<T>>";

        /**
         * 分页响应
         * 格式：Result<PageResponse<T>>
         */
        public static final String PAGE = "Result<PageResponse<T>>";

        /**
         * 布尔值响应（操作成功/失败）
         * 格式：Result<Boolean>
         */
        public static final String BOOLEAN = "Result<Boolean>";

        /**
         * 整数响应（数量等）
         * 格式：Result<Integer>
         */
        public static final String INTEGER = "Result<Integer>";
    }

    /**
     * 接口命名规范说明
     * 
     * 1. GET 接口（单条查询）
     *    - 路径：/{entity}/get
     *    - 方法：POST
     *    - 入参：IdRequest（包含 id 字段）
     *    - 出参：Result<T>
     * 
     * 2. LIST_ALL 接口（列表查询 - 不分页）
     *    - 路径：/{entity}/list/all
     *    - 方法：POST
     *    - 入参：ListRequest（可包含查询条件、排序）
     *    - 出参：Result<List<T>>
     * 
     * 3. LIST_PAGE 接口（分页查询）
     *    - 路径：/{entity}/list/page
     *    - 方法：POST
     *    - 入参：PageRequest（包含 pageNum、pageSize、查询条件、排序）
     *    - 出参：Result<PageResponse<T>>
     * 
     * 4. SAVE 接口（保存）
     *    - 路径：/{entity}/save
     *    - 方法：POST
     *    - 入参：SaveRequest（包含保存的字段）
     *    - 出参：Result<Boolean>
     * 
     * 5. UPDATE 接口（更新）
     *    - 路径：/{entity}/update
     *    - 方法：POST
     *    - 入参：UpdateRequest（包含 id 和更新的字段）
     *    - 出参：Result<Boolean>
     * 
     * 6. DELETE 接口（删除）
     *    - 路径：/{entity}/delete
     *    - 方法：POST
     *    - 入参：IdRequest（包含 id 字段）
     *    - 出参：Result<Boolean>
     */
}
