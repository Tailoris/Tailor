package com.tailoris.order.config;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 商品数据库 JdbcTemplate 配置.
 *
 * <p>为订单服务的库存扣减操作提供对 tailor_is_product 数据库的访问能力。
 * 不暴露 DataSource Bean 以避免干扰 MyBatis 主数据源自动配置。</p>
 *
 * @author Tailor IS Team
 */
@Slf4j
@Configuration
public class ProductDataSourceConfig {

    @Value("${spring.datasource.product.url:jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/tailor_is_product?useUnicode=true&characterEncoding=utf-8&useSSL=true&serverTimezone=Asia/Shanghai}")
    private String url;

    @Value("${spring.datasource.product.username:${MYSQL_USERNAME:root}}")
    private String username;

    @Value("${spring.datasource.product.password:${MYSQL_PASSWORD:mysql_ZmY2sr}}")
    private String password;

    @Bean(name = "productJdbcTemplate", destroyMethod = "")
    public JdbcTemplate productJdbcTemplate() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setInitialSize(2);
        dataSource.setMinIdle(2);
        dataSource.setMaxActive(10);
        dataSource.setMaxWait(30000);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        log.info("Product JdbcTemplate initialized for tailor_is_product database");
        return new JdbcTemplate(dataSource);
    }
}