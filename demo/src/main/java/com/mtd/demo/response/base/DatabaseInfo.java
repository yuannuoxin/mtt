package com.mtd.demo.response.base;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据库信息
 */
@Data
public class DatabaseInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据库时区 (@@time_zone)
     */
    private String dbTimezone;

    /**
     * 数据库系统时区 (@@system_time_zone)
     */
    private String dbSystemTimezone;

    /**
     * 数据库字符集 (@@character_set_database)
     */
    private String dbCharset;

    /**
     * 备注
     */
    private String note;
}
