package com.tailoris.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageRequest {

    @Min(value = 1, message = "页码最小为1")
    private int pageNum = 1;

    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private int pageSize = 10;

    private String orderBy;

    private String orderDirection;

    public PageRequest() {
    }

    public PageRequest(int pageNum, int pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public PageRequest(Integer pageNum, Integer pageSize) {
        this.pageNum = pageNum != null ? pageNum : 1;
        this.pageSize = pageSize != null ? pageSize : 10;
    }

    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }

    public boolean hasOrderBy() {
        return orderBy != null && !orderBy.trim().isEmpty();
    }

    public String getSafeOrderDirection() {
        if (orderDirection == null) {
            return "ASC";
        }
        String direction = orderDirection.trim().toUpperCase();
        if ("DESC".equals(direction)) {
            return "DESC";
        }
        return "ASC";
    }
}