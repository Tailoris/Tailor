package com.tailoris.common.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PageResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> records;

    private long total;

    @JsonProperty("current")
    private int pageNum;

    @JsonProperty("size")
    private int pageSize;

    @JsonProperty("pages")
    private int totalPages;

    public PageResponse() {
    }

    public PageResponse(List<T> records, long total, int pageNum, int pageSize) {
        this.records = records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
    }

    public boolean hasNext() {
        return pageNum < totalPages;
    }

    public boolean hasPrevious() {
        return pageNum > 1;
    }

    public static <T> PageResponse<T> empty(int pageNum, int pageSize) {
        return new PageResponse<>(List.of(), 0, pageNum, pageSize);
    }

    public static <T> PageResponse<T> of(List<T> records, long total, int pageNum, int pageSize) {
        return new PageResponse<>(records, total, pageNum, pageSize);
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getRecords(), page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }
}