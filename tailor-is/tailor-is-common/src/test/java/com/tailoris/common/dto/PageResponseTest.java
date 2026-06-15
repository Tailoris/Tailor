package com.tailoris.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("分页响应测试")
class PageResponseTest {

    @Test
    @DisplayName("默认构造")
    void testDefaultConstructor() {
        PageResponse<String> response = new PageResponse<>();
        assertNull(response.getRecords());
        assertEquals(0, response.getTotal());
        assertEquals(0, response.getPageNum());
        assertEquals(0, response.getPageSize());
        assertEquals(0, response.getTotalPages());
    }

    @Test
    @DisplayName("带参数构造")
    void testConstructorWithParams() {
        List<String> records = Arrays.asList("a", "b", "c");
        PageResponse<String> response = new PageResponse<>(records, 100, 1, 10);

        assertEquals(records, response.getRecords());
        assertEquals(100, response.getTotal());
        assertEquals(1, response.getPageNum());
        assertEquals(10, response.getPageSize());
        assertEquals(10, response.getTotalPages());
    }

    @Test
    @DisplayName("计算总页数 - 整除")
    void testTotalPagesExact() {
        PageResponse<String> response = new PageResponse<>(Collections.emptyList(), 100, 1, 10);
        assertEquals(10, response.getTotalPages());
    }

    @Test
    @DisplayName("计算总页数 - 有余数")
    void testTotalPagesWithRemainder() {
        PageResponse<String> response = new PageResponse<>(Collections.emptyList(), 101, 1, 10);
        assertEquals(11, response.getTotalPages());
    }

    @Test
    @DisplayName("计算总页数 - pageSize为0")
    void testTotalPagesZeroPageSize() {
        PageResponse<String> response = new PageResponse<>(Collections.emptyList(), 100, 1, 0);
        assertEquals(0, response.getTotalPages());
    }

    @Test
    @DisplayName("是否有下一页 - 有")
    void testHasNextTrue() {
        PageResponse<String> response = new PageResponse<>(Collections.emptyList(), 100, 1, 10);
        assertTrue(response.hasNext());
    }

    @Test
    @DisplayName("是否有下一页 - 无（最后一页）")
    void testHasNextFalse() {
        PageResponse<String> response = new PageResponse<>(Collections.emptyList(), 100, 10, 10);
        assertFalse(response.hasNext());
    }

    @Test
    @DisplayName("是否有上一页 - 有")
    void testHasPreviousTrue() {
        PageResponse<String> response = new PageResponse<>(Collections.emptyList(), 100, 2, 10);
        assertTrue(response.hasPrevious());
    }

    @Test
    @DisplayName("是否有上一页 - 无（第一页）")
    void testHasPreviousFalse() {
        PageResponse<String> response = new PageResponse<>(Collections.emptyList(), 100, 1, 10);
        assertFalse(response.hasPrevious());
    }

    @Test
    @DisplayName("空分页")
    void testEmpty() {
        PageResponse<String> response = PageResponse.empty(1, 10);
        assertNotNull(response.getRecords());
        assertTrue(response.getRecords().isEmpty());
        assertEquals(0, response.getTotal());
        assertEquals(1, response.getPageNum());
        assertEquals(10, response.getPageSize());
        assertEquals(0, response.getTotalPages());
        assertFalse(response.hasNext());
        assertFalse(response.hasPrevious());
    }

    @Test
    @DisplayName("getter/setter")
    void testGettersSetters() {
        PageResponse<String> response = new PageResponse<>();
        List<String> records = Arrays.asList("a", "b");
        response.setRecords(records);
        response.setTotal(50);
        response.setPageNum(2);
        response.setPageSize(10);
        response.setTotalPages(5);

        assertEquals(records, response.getRecords());
        assertEquals(50, response.getTotal());
        assertEquals(2, response.getPageNum());
        assertEquals(10, response.getPageSize());
        assertEquals(5, response.getTotalPages());
    }

    @Test
    @DisplayName("单条记录")
    void testSingleRecord() {
        List<String> records = Collections.singletonList("single");
        PageResponse<String> response = new PageResponse<>(records, 1, 1, 10);
        assertEquals(1, response.getRecords().size());
        assertEquals("single", response.getRecords().get(0));
        assertEquals(1, response.getTotalPages());
    }

    @Test
    @DisplayName("边界值 - total为0")
    void testTotalZero() {
        PageResponse<String> response = new PageResponse<>(Collections.emptyList(), 0, 1, 10);
        assertEquals(0, response.getTotal());
        assertEquals(0, response.getTotalPages());
        assertFalse(response.hasNext());
    }
}
