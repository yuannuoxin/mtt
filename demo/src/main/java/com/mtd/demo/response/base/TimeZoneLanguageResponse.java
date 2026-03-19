package com.mtd.demo.response.base;

import lombok.Data;

import java.io.Serializable;

/**
 * 时区与语言信息响应对象
 */
@Data
public class TimeZoneLanguageResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 客户端信息
     */
    private ClientInfo client;

    /**
     * 服务器 (JVM) 信息
     */
    private ServerInfo server;

    /**
     * 数据库 (MySQL) 信息
     */
    private DatabaseInfo database;
}
