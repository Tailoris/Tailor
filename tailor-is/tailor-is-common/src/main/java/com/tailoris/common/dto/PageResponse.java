package com.tailoris.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PageResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> records;

    private long total;

    private int pageNum;

    private int pageSize;

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
}