package com.tailoris.ai.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.tailoris.ai.dto.SizeDataRequest;
import com.tailoris.ai.entity.BodySizeData;
import com.tailoris.ai.mapper.BodySizeDataMapper;
import com.tailoris.ai.service.BodySizeService;
import com.tailoris.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@DisplayName("BodySizeServiceImpl 集成测试（需MyBatis-Plus lambda缓存）")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = BodySizeServiceImplIntegrationTest.TestConfig.class)
@Sql(scripts = "classpath:schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Transactional
class BodySizeServiceImplIntegrationTest {

    @Autowired
    private BodySizeService bodySizeService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("新增默认体型数据时清除其他默认标记（LambdaUpdateWrapper）")
    void testManageSizeData_SetDefault() {
        SizeDataRequest request = buildRequest("默认体型", 1);

        BodySizeData result = bodySizeService.manageSizeData(1L, request);

        assertNotNull(result);
        assertEquals(1, result.getIsDefault());
        assertEquals("默认体型", result.getSizeName());
        assertNotNull(result.getId());
    }

    @Test
    @DisplayName("设置默认体型数据（LambdaUpdateWrapper）")
    void testSetDefaultSize_Success() {
        SizeDataRequest request = buildRequest("临时体型", 0);
        BodySizeData created = bodySizeService.manageSizeData(1L, request);

        bodySizeService.setDefaultSize(1L, created.getId());

        BodySizeData updated = bodySizeService.getSizeData(created.getId());
        assertNotNull(updated);
        assertEquals(1, updated.getIsDefault());
    }

    @Test
    @DisplayName("获取不存在的体型数据返回null")
    void testGetSizeData_NotFound() {
        BodySizeData result = bodySizeService.getSizeData(99999L);
        assertNull(result);
    }

    @Test
    @DisplayName("删除他人数据抛出异常")
    void testDelete_NotOwner() {
        SizeDataRequest request = buildRequest("用户2的体型", 0);
        BodySizeData created = bodySizeService.manageSizeData(1L, request);

        assertThrows(BusinessException.class, () ->
                bodySizeService.deleteSizeData(2L, created.getId()));
    }

    private SizeDataRequest buildRequest(String name, int isDefault) {
        SizeDataRequest request = new SizeDataRequest();
        request.setSizeName(name);
        request.setHeight(new BigDecimal("170"));
        request.setWeight(new BigDecimal("65"));
        request.setShoulderWidth(new BigDecimal("44"));
        request.setChestCircumference(new BigDecimal("96"));
        request.setWaistCircumference(new BigDecimal("82"));
        request.setHipCircumference(new BigDecimal("95"));
        request.setGender(1);
        request.setBodyType("normal");
        request.setIsDefault(isDefault);
        return request;
    }

    @Configuration
    @Import({DataSourceAutoConfiguration.class, MybatisPlusAutoConfiguration.class})
    @MapperScan("com.tailoris.ai.mapper")
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        DataSource dataSource() {
            return DataSourceBuilder.create()
                    .type(org.springframework.jdbc.datasource.SimpleDriverDataSource.class)
                    .url("jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                    .username("sa")
                    .password("")
                    .driverClassName("org.h2.Driver")
                    .build();
        }

        @Bean
        MybatisSqlSessionFactoryBean sqlSessionFactory(DataSource dataSource) throws Exception {
            MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
            factoryBean.setDataSource(dataSource);
            return factoryBean;
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        BodySizeServiceImpl bodySizeService(BodySizeDataMapper bodySizeDataMapper,
                                              StringRedisTemplate stringRedisTemplate) {
            return new BodySizeServiceImpl(bodySizeDataMapper, stringRedisTemplate);
        }

        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return org.mockito.Mockito.mock(StringRedisTemplate.class);
        }

        @Bean
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations() {
            return org.mockito.Mockito.mock(ValueOperations.class);
        }
    }
}