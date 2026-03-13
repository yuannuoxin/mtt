package com.mtd.common.core.constant;

/**
 * 通用常量类
 * 定义项目中通用的常量
 */
public class CommonConstants {

    /**
     * 排序相关常量
     */
    public static class SortOrder {
        /**
         * 升序
         */
        public static final String ASC = "asc";
        
        /**
         * 降序
         */
        public static final String DESC = "desc";
    }
    
    /**
     * 用户状态常量
     */
    public static class UserStatus {
        /**
         * 禁用
         */
        public static final Integer DISABLED = 0;
        
        /**
         * 正常
         */
        public static final Integer NORMAL = 1;
    }
}
