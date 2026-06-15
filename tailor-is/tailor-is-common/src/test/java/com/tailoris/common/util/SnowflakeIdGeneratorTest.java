package com.tailoris.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("雪花ID生成器测试")
class SnowflakeIdGeneratorTest {

    @Test
    @DisplayName("创建实例 - 正常参数")
    void testCreateInstance() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        assertNotNull(generator);
        assertEquals(1, generator.getWorkerId());
        assertEquals(1, generator.getDatacenterId());
    }

    @Test
    @DisplayName("创建实例 - 边界值")
    void testCreateInstanceBoundary() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(0, 0);
        assertEquals(0, generator.getWorkerId());
        assertEquals(0, generator.getDatacenterId());

        SnowflakeIdGenerator maxGenerator = new SnowflakeIdGenerator(31, 31);
        assertEquals(31, maxGenerator.getWorkerId());
        assertEquals(31, maxGenerator.getDatacenterId());
    }

    @Test
    @DisplayName("创建实例 - workerId 超出范围")
    void testCreateInstanceWorkerIdOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SnowflakeIdGenerator(32, 1);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new SnowflakeIdGenerator(-1, 1);
        });
    }

    @Test
    @DisplayName("创建实例 - datacenterId 超出范围")
    void testCreateInstanceDatacenterIdOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SnowflakeIdGenerator(1, 32);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new SnowflakeIdGenerator(1, -1);
        });
    }

    @Test
    @DisplayName("生成ID - 唯一性")
    void testNextIdUniqueness() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        long id1 = generator.nextId();
        long id2 = generator.nextId();
        long id3 = generator.nextId();

        assertNotEquals(id1, id2);
        assertNotEquals(id2, id3);
        assertNotEquals(id1, id3);
    }

    @Test
    @DisplayName("生成ID - 递增性")
    void testNextIdIncremental() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        long id1 = generator.nextId();
        long id2 = generator.nextId();
        long id3 = generator.nextId();

        assertTrue(id1 < id2);
        assertTrue(id2 < id3);
    }

    @Test
    @DisplayName("生成ID - 正数")
    void testNextIdPositive() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        for (int i = 0; i < 100; i++) {
            long id = generator.nextId();
            assertTrue(id > 0);
        }
    }

    @Test
    @DisplayName("获取单例实例")
    void testGetInstance() {
        SnowflakeIdGenerator instance1 = SnowflakeIdGenerator.getInstance();
        SnowflakeIdGenerator instance2 = SnowflakeIdGenerator.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    @DisplayName("获取指定参数的实例")
    void testGetInstanceWithParams() {
        SnowflakeIdGenerator generator = SnowflakeIdGenerator.getInstance(5, 10);
        assertNotNull(generator);
        assertEquals(5, generator.getWorkerId());
        assertEquals(10, generator.getDatacenterId());
    }

    @Test
    @DisplayName("批量生成ID - 性能测试")
    void testBatchGenerateIds() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        int count = 1000;
        long[] ids = new long[count];

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            ids[i] = generator.nextId();
        }
        long endTime = System.currentTimeMillis();

        // 验证唯一性
        for (int i = 0; i < count; i++) {
            for (int j = i + 1; j < count; j++) {
                assertNotEquals(ids[i], ids[j]);
            }
        }

        // 验证性能（1000个ID应在1秒内生成）
        assertTrue(endTime - startTime < 1000);
    }

    @Test
    @DisplayName("不同workerId生成不同ID")
    void testDifferentWorkerId() {
        SnowflakeIdGenerator gen1 = new SnowflakeIdGenerator(1, 1);
        SnowflakeIdGenerator gen2 = new SnowflakeIdGenerator(2, 1);

        long id1 = gen1.nextId();
        long id2 = gen2.nextId();

        assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("不同datacenterId生成不同ID")
    void testDifferentDatacenterId() {
        SnowflakeIdGenerator gen1 = new SnowflakeIdGenerator(1, 1);
        SnowflakeIdGenerator gen2 = new SnowflakeIdGenerator(1, 2);

        long id1 = gen1.nextId();
        long id2 = gen2.nextId();

        assertNotEquals(id1, id2);
    }
}
