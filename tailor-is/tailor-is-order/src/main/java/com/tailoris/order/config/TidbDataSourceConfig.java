package com.tailoris.order.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * TiDB 数据源配置.
 *
 * <p>当 {@code tidb} profile 激活或 {@code tidb.enabled=true} 时生效，
 * 创建一个独立的 DataSource 连接 TiDB，作为订单主数据源。</p>
 *
 * <p>TiDB 完全兼容 MySQL 协议，可直接使用 MySQL JDBC 驱动连接。
 * 配置中启用了 TiDB 推荐的连接参数：
 *   - rewriteBatchedStatements=true: 批量写入优化
 *   - useServerPrepStmts=true: 服务端预处理
 *   - cachePrepStmts=true: 预处理语句缓存
 * </p>
 */
@Slf4j
@Configuration
@Profile("tidb")
@ConditionalOnProperty(name = "tidb.enabled", havingValue = "true", matchIfMissing = true)
@MapperScan(basePackages = "com.tailoris.order.mapper",
        sqlSessionTemplateRef = "tidbSqlSessionTemplate")
public class TidbDataSourceConfig {

    /**
     * 创建 TiDB 数据源，标记为 @Primary 以替代默认数据源.
     */
    @Primary
    @Bean(name = "tidbDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.tidb")
    public DataSource tidbDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setInitialSize(10);
        dataSource.setMinIdle(10);
        dataSource.setMaxActive(50);
        dataSource.setMaxWait(60000);
        dataSource.setTimeBetweenEvictionRunsMillis(60000);
        dataSource.setMinEvictableIdleTimeMillis(300000);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        dataSource.setPoolPreparedStatements(true);
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(20);
        log.info("TiDB DataSource initialized with Druid connection pool");
        return dataSource;
    }

    /**
     * 创建 TiDB 的 MyBatis-Plus SqlSessionFactory.
     */
    @Primary
    @Bean(name = "tidbSqlSessionFactory")
    public SqlSessionFactory tidbSqlSessionFactory(
            @Qualifier("tidbDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:/mapper/**/*.xml"));
        factoryBean.setTypeAliasesPackage("com.tailoris.order.entity");

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);

        return factoryBean.getObject();
    }

    /**
     * 创建 TiDB 的 SqlSessionTemplate.
     */
    @Primary
    @Bean(name = "tidbSqlSessionTemplate")
    public SqlSessionTemplate tidbSqlSessionTemplate(
            @Qualifier("tidbSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    /**
     * 创建 TiDB 的事务管理器.
     */
    @Primary
    @Bean(name = "tidbTransactionManager")
    public DataSourceTransactionManager tidbTransactionManager(
            @Qualifier("tidbDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}