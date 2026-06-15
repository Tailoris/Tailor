package com.tailoris.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring Snowflake ID生成器测试 - 覆盖 B-H24
 *
 * @author Tailor IS Team
 */
@DisplayName("SpringSnowflakeIdGenerator 测试")
class SpringSnowflakeIdGeneratorTest {

    private SpringSnowflakeIdGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SpringSnowflakeIdGenerator();
        ReflectionTestUtils.setField(generator, "workerId", 5);
        ReflectionTestUtils.setField(generator, "datacenterId", 5);
    }

    @Test
    @DisplayName("成功初始化并生成ID")
    void shouldInitAndGenerateId() {
        generator.init();
        long id = generator.nextId();
        assertTrue(id > 0);
        assertEquals(5, generator.getWorkerId());
        assertEquals(5, generator.getDatacenterId());
    }

    @Test
    @DisplayName("workerId 超出范围抛出异常")
    void shouldThrowForInvalidWorkerId() {
        ReflectionTestUtils.setField(generator, "workerId", 32);
        assertThrows(IllegalArgumentException.class, generator::init);
    }

    @Test
    @DisplayName("workerId 为负数抛出异常")
    void shouldThrowForNegativeWorkerId() {
        ReflectionTestUtils.setField(generator, "workerId", -1);
        assertThrows(IllegalArgumentException.class, generator::init);
    }

    @Test
    @DisplayName("datacenterId 超出范围抛出异常")
    void shouldThrowForInvalidDatacenterId() {
        ReflectionTestUtils.setField(generator, "datacenterId", 32);
        assertThrows(IllegalArgumentException.class, generator::init);
    }

    @Test
    @DisplayName("生成多个唯一ID")
    void shouldGenerateUniqueIds() {
        generator.init();
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(generator.nextId());
        }
        assertEquals(1000, ids.size(), "生成的ID应全部唯一");
    }

    @Test
    @DisplayName("多实例使用不同配置")
    void shouldSupportMultipleInstances() {
        SpringSnowflakeIdGenerator gen1 = new SpringSnowflakeIdGenerator();
        ReflectionTestUtils.setField(gen1, "workerId", 1);
        ReflectionTestUtils.setField(gen1, "datacenterId", 1);
        gen1.init();

        SpringSnowflakeIdGenerator gen2 = new SpringSnowflakeIdGenerator();
        ReflectionTestUtils.setField(gen2, "workerId", 2);
        ReflectionTestUtils.setField(gen2, "datacenterId", 1);
        gen2.init();

        long id1 = gen1.nextId();
        long id2 = gen2.nextId();
        assertNotEquals(id1, id2);
    }
}
