package com.tailoris.common.util;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 批量查询工具测试 - 覆盖 B-H18/TD-04
 *
 * @author Tailor IS Team
 */
@DisplayName("BatchQueryUtil 测试")
class BatchQueryUtilTest {

    static class TestEntity {
        private final Long id;
        private final String name;

        TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
    }

    @Test
    @DisplayName("批量查询返回ID到实体的映射")
    void shouldBatchGetEntities() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        MockService<TestEntity> mockService = new MockService<>(Arrays.asList(
                new TestEntity(1L, "Alice"),
                new TestEntity(2L, "Bob"),
                new TestEntity(3L, "Charlie")
        ));

        Map<Long, TestEntity> result = BatchQueryUtil.batchGet(mockService, ids, TestEntity::getId);
        assertEquals(3, result.size());
        assertEquals("Alice", result.get(1L).getName());
        assertEquals("Bob", result.get(2L).getName());
        assertEquals("Charlie", result.get(3L).getName());
    }

    @Test
    @DisplayName("空ID集合返回空Map")
    void shouldReturnEmptyMapForNullIds() {
        MockService<TestEntity> mockService = new MockService<>(Collections.emptyList());
        Map<Long, TestEntity> result = BatchQueryUtil.batchGet(mockService, null, TestEntity::getId);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("空ID列表返回空Map")
    void shouldReturnEmptyMapForEmptyIds() {
        MockService<TestEntity> mockService = new MockService<>(Collections.emptyList());
        Map<Long, TestEntity> result = BatchQueryUtil.batchGet(mockService, Collections.emptyList(), TestEntity::getId);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("查询异常时返回空Map")
    void shouldReturnEmptyMapOnException() {
        MockService<TestEntity> brokenService = new MockService<TestEntity>(Collections.emptyList()) {
            @Override
            public List<TestEntity> listByIds(Collection<? extends Serializable> idList) {
                throw new RuntimeException("DB error");
            }
        };

        Map<Long, TestEntity> result = BatchQueryUtil.batchGet(
                brokenService, Arrays.asList(1L, 2L), TestEntity::getId);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("批量赋值方法")
    void shouldBatchAssign() {
        List<TestEntity> sources = Arrays.asList(
                new TestEntity(1L, "source1"),
                new TestEntity(2L, "source2")
        );
        MockService<TestEntity> targetService = new MockService<>(Arrays.asList(
                new TestEntity(1L, "target1"),
                new TestEntity(2L, "target2")
        ));

        java.util.HashMap<Long, String> assignResult = new java.util.HashMap<>();
        BatchQueryUtil.batchAssign(
                sources,
                TestEntity::getId,
                targetService,
                TestEntity::getId,
                (s, t) -> {
                    if (t != null) {
                        assignResult.put(s.getId(), t.getName());
                    }
                }
        );

        assertEquals(2, assignResult.size());
        assertEquals("target1", assignResult.get(1L));
        assertEquals("target2", assignResult.get(2L));
    }

    @Test
    @DisplayName("空源列表不执行查询")
    void shouldSkipBatchAssignForEmptySource() {
        MockService<TestEntity> mockService = new MockService<>(Collections.emptyList());
        // 不应该抛异常
        assertDoesNotThrow(() -> BatchQueryUtil.batchAssign(
                new ArrayList<>(),
                TestEntity::getId,
                mockService,
                TestEntity::getId,
                (s, t) -> {}
        ));
    }

    @Test
    @DisplayName("分页批量查询")
    void shouldBatchPage() {
        List<TestEntity> allData = Arrays.asList(
                new TestEntity(1L, "A"),
                new TestEntity(2L, "B"),
                new TestEntity(3L, "C")
        );
        MockService<TestEntity> mockService = new MockService<>(allData);
        IPage<TestEntity> result = BatchQueryUtil.batchPage(mockService, 1, 10, null);
        assertNotNull(result);
        assertEquals(3L, result.getTotal());
    }

    /**
     * Mock Service 实现 - 基于 ServiceImpl 避免手动实现 IService 所有方法。
     * MyBatis-Plus 3.5.7 IService 接口含泛型 page 方法签名和 getEntityClass() 抽象方法，
     * 直接实现 IService 会产生名称冲突和返回类型不兼容问题。
     */
    static class MockService<T> extends ServiceImpl<BaseMapper<T>, T> {

        private final List<T> data;

        MockService(List<T> data) {
            this.data = data;
        }

        @Override
        public List<T> listByIds(Collection<? extends Serializable> idList) {
            return new ArrayList<>(data);
        }

        @Override
        public List<T> list() {
            return new ArrayList<>(data);
        }

        @Override
        public <P extends IPage<T>> P page(P page, Wrapper<T> queryWrapper) {
            Page<T> p = new Page<>(page.getCurrent(), page.getSize());
            p.setRecords(new ArrayList<>(data));
            p.setTotal(data.size());
            @SuppressWarnings("unchecked")
            P result = (P) p;
            return result;
        }

        @Override
        public boolean save(T entity) {
            return SqlHelper.retBool(1);
        }
    }
}