package com.tailoris.common.interceptor;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import org.apache.ibatis.mapping.BoundSql;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.ibatis.executor.statement.StatementHandler;

/**
 * 数据权限 MyBatis Plus 内部拦截器 - 在 SQL 执行前自动设置 DataPermissionContext 中的表名。
 * <p>
 * 实现 MyBatis Plus 的 InnerInterceptor 接口，在 MybatisPlusInterceptor 链中
 * 位于 DataPermissionInterceptor 之前执行，确保 DataPermissionHandlerImpl
 * 可以通过 ThreadLocal 获取到当前操作的表名。
 * </p>
 * <p>
 * 注意：DataPermissionHandlerImpl 已优先通过 mappedStatementId + TableInfoHelper
 * 直接获取表名，此拦截器作为补充机制，同时可供其他需要获取当前表名的组件使用。
 * </p>
 *
 * @author Tailoris
 */
@Slf4j
public class DataPermissionMybatisInterceptor implements InnerInterceptor {

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        setCurrentTable(ms);
    }

    @Override
    public void beforeUpdate(Executor executor, MappedStatement ms, Object parameter) throws SQLException {
        setCurrentTable(ms);
    }

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        // SQL 已被 DataPermissionInterceptor 处理完毕，清除 ThreadLocal
        DataPermissionContext.clear();
    }

    /**
     * 从 MappedStatement ID 解析表名并设置到 ThreadLocal
     *
     * @param ms MyBatis MappedStatement
     */
    private void setCurrentTable(MappedStatement ms) {
        try {
            String mappedStatementId = ms.getId();
            if (mappedStatementId != null) {
                for (TableInfo tableInfo : TableInfoHelper.getTableInfos()) {
                    String namespace = tableInfo.getCurrentNamespace();
                    if (namespace != null && mappedStatementId.startsWith(namespace + ".")) {
                        DataPermissionContext.setCurrentTable(tableInfo.getTableName());
                        log.debug("数据权限拦截器 - 设置当前表名: {}", tableInfo.getTableName());
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("数据权限拦截器 - 设置表名失败: {}", e.getMessage());
        }
    }
}