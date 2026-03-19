package com.mtd.demo.response.base;

import lombok.Data;

import java.io.Serializable;

/**
 * 客户端信息
 */
@Data
public class ClientInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 客户端 IP
     */
    private String clientIp;

    /**
     * 客户端语言
     */
    private String clientLanguage;

    /**
     * 客户端 User-Agent
     */
    private String clientUserAgent;

    /**
     * 客户端时区
     */
    private String timezone;

    /**
     * 备注
     */
    private String note;
}
