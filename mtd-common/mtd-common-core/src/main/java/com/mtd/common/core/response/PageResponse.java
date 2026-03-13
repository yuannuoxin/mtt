package com.mtd.common.core.response;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 统一分页响应结果
 */
@Data
public class PageResponse<T> implements Serializable {

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Integer pages;

    /**
     * 数据列表
     */
    private List<T> list;

    public PageResponse() {
    }

    public PageResponse(Integer pageNum, Integer pageSize, Long total, List<T> list) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.list = list;
        this.pages = (int) Math.ceil((double) total / pageSize);
    }

    public static <T> PageResponse<T> of(Integer pageNum, Integer pageSize, Long total, List<T> list) {
        return new PageResponse<>(pageNum, pageSize, total, list);
    }
}
