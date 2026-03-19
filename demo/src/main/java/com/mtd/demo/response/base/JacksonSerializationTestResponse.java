package com.mtd.demo.response.base;

import cn.hutool.core.date.DateTime;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

/**
 * Jackson 序列化测试响应对象
 */
@Data
public class JacksonSerializationTestResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Long 类型（应转为 String）
     */
    private Long longValue;

    /**
     * long 基本类型（应转为 String）
     */
    private long longPrimitive;

    /**
     * Integer 类型（保持数字）
     */
    private Integer integerValue;

    /**
     * LocalDateTime（应格式化为 yyyy-MM-dd HH:mm:ss）
     */
    private LocalDateTime localDateTime;

    /**
     * LocalDate（应格式化为 yyyy-MM-dd）
     */
    private LocalDate localDate;

    /**
     * LocalTime（应格式化为 HH:mm:ss）
     */
    private LocalTime localTime;

    /**
     * java.util.Date（应格式化为 yyyy-MM-dd HH:mm:ss）
     */
    private Date utilDate;

    /**
     * java.sql.Timestamp（应格式化为 yyyy-MM-dd HH:mm:ss）
     */
    private Timestamp timestamp;

    /**
     * BigInteger（应转为 String）
     */
    private BigInteger bigInteger;

    /**
     * BigDecimal（应转为 String）
     */
    private BigDecimal bigDecimal;

    /**
     * 说明信息
     */
    private String note;
}
