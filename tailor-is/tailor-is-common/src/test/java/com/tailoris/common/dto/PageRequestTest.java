package com.tailoris.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("分页请求测试")
class PageRequestTest {

    @Test
    @DisplayName("默认构造")
    void testDefaultConstructor() {
        PageRequest request = new PageRequest();
        assertEquals(1, request.getPageNum());
        assertEquals(10, request.getPageSize());
        assertNull(request.getOrderBy());
        assertNull(request.getOrderDirection());
    }

    @Test
    @DisplayName("带参数构造 - int")
    void testConstructorWithInt() {
        PageRequest request = new PageRequest(2, 20);
        assertEquals(2, request.getPageNum());
        assertEquals(20, request.getPageSize());
    }

    @Test
    @DisplayName("带参数构造 - Integer")
    void testConstructorWithInteger() {
        PageRequest request = new PageRequest(3, 30);
        assertEquals(3, request.getPageNum());
        assertEquals(30, request.getPageSize());
    }

    @Test
    @DisplayName("带参数构造 - null值")
    void testConstructorWithNull() {
        PageRequest request = new PageRequest(null, null);
        assertEquals(1, request.getPageNum());
        assertEquals(10, request.getPageSize());
    }

    @Test
    @DisplayName("计算偏移量")
    void testGetOffset() {
        PageRequest request = new PageRequest(1, 10);
        assertEquals(0, request.getOffset());

        request.setPageNum(2);
        assertEquals(10, request.getOffset());

        request.setPageNum(3);
        request.setPageSize(20);
        assertEquals(40, request.getOffset());
    }

    @Test
    @DisplayName("是否有排序字段")
    void testHasOrderBy() {
        PageRequest request = new PageRequest();
        assertFalse(request.hasOrderBy());

        request.setOrderBy("id");
        assertTrue(request.hasOrderBy());

        request.setOrderBy("  ");
        assertFalse(request.hasOrderBy());
    }

    @Test
    @DisplayName("获取安全的排序方向 - null")
    void testGetSafeOrderDirectionNull() {
        PageRequest request = new PageRequest();
        assertEquals("ASC", request.getSafeOrderDirection());
    }

    @Test
    @DisplayName("获取安全的排序方向 - DESC")
    void testGetSafeOrderDirectionDesc() {
        PageRequest request = new PageRequest();
        request.setOrderDirection("DESC");
        assertEquals("DESC", request.getSafeOrderDirection());

        request.setOrderDirection("desc");
        assertEquals("DESC", request.getSafeOrderDirection());

        request.setOrderDirection("  DESC  ");
        assertEquals("DESC", request.getSafeOrderDirection());
    }

    @Test
    @DisplayName("获取安全的排序方向 - ASC")
    void testGetSafeOrderDirectionAsc() {
        PageRequest request = new PageRequest();
        request.setOrderDirection("ASC");
        assertEquals("ASC", request.getSafeOrderDirection());

        request.setOrderDirection("asc");
        assertEquals("ASC", request.getSafeOrderDirection());
    }

    @Test
    @DisplayName("获取安全的排序方向 - 无效值")
    void testGetSafeOrderDirectionInvalid() {
        PageRequest request = new PageRequest();
        request.setOrderDirection("INVALID");
        assertEquals("ASC", request.getSafeOrderDirection());
    }

    @Test
    @DisplayName("getter/setter")
    void testGettersSetters() {
        PageRequest request = new PageRequest();
        request.setPageNum(5);
        request.setPageSize(50);
        request.setOrderBy("name");
        request.setOrderDirection("DESC");

        assertEquals(5, request.getPageNum());
        assertEquals(50, request.getPageSize());
        assertEquals("name", request.getOrderBy());
        assertEquals("DESC", request.getOrderDirection());
    }

    @Test
    @DisplayName("边界值测试")
    void testBoundaryValues() {
        PageRequest request = new PageRequest(1, 1);
        assertEquals(0, request.getOffset());

        request.setPageNum(100);
        request.setPageSize(100);
        assertEquals(9900, request.getOffset());
    }
}
