package com.mtd.demo.response.base;

import cn.hutool.core.date.DateTime;
import lombok.Data;

import java.io.Serializable;
import java.time.*;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

/**
 * 日期类型响应对象
 */
@Data
public class DateTypesResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * java.util.Date
     */
    private Date utilDate;

    /**
     * java.sql.Date
     */
    private Date sqlDate;

    /**
     * java.sql.Timestamp
     */
    private Timestamp timestamp;

    /**
     * java.sql.Time
     */
    private Time time;

    /**
     * LocalDate (yyyy-MM-dd)
     */
    private LocalDate localDate;

    /**
     * LocalTime (HH:mm:ss)
     */
    private LocalTime localTime;

    /**
     * LocalDateTime (yyyy-MM-dd HH:mm:ss)
     */
    private LocalDateTime localDateTime;

    /**
     * Instant (时间戳)
     */
    private Instant instant;

    /**
     * ZonedDateTime (带时区的时间)
     */
    private ZonedDateTime zonedDateTime;

    /**
     * OffsetDateTime (带偏移量的时间)
     */
    private OffsetDateTime offsetDateTime;

    /**
     * YearMonth (年月)
     */
    private YearMonth yearMonth;

    /**
     * MonthDay (月日)
     */
    private MonthDay monthDay;

    /**
     * Year (年)
     */
    private Year year;

    /**
     * Hutool DateTime
     */
    private DateTime hutoolDateTime;

    /**
     * 毫秒时间戳 (System.currentTimeMillis)
     */
    private Long currentTimeMillis;

    /**
     * 纳秒时间戳 (System.nanoTime)
     */
    private Long nanoTime;
}
