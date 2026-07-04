package com.tailoris.common.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.tailoris.common.constant.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class DataPermissionHandlerImpl implements DataPermissionHandler {

    private static final Set<String> DATA_SCOPE_TABLES = new HashSet<>(Arrays.asList(
            "order_info", "product", "merchant", "community_post",
            "supply_demand_post", "coupon_template", "copyright_record"
    ));

    private static final Set<String> MERCHANT_SCOPE_TABLES = new HashSet<>(Arrays.asList(
            "product", "order_info", "coupon_template", "shop_member"
    ));

    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            if (loginId == null) {
                return where;
            }

            boolean isAdmin = StpUtil.hasRole("admin");
            if (isAdmin) {
                return where;
            }

            Long userId = StpUtil.getLoginIdAsLong();
            String userType = StpUtil.getLoginDevice();
            boolean isMerchant = CommonConstants.USER_TYPE_MERCHANT.equals(userType);

            Table table = getSelectTable(mappedStatementId);
            if (table == null) {
                return where;
            }

            String tableName = table.getName().toLowerCase();
            Expression scopeExpression = null;

            if (isMerchant && MERCHANT_SCOPE_TABLES.contains(tableName)) {
                scopeExpression = buildMerchantScope(table, userId);
            } else if (!isMerchant && DATA_SCOPE_TABLES.contains(tableName)) {
                scopeExpression = buildUserScope(table, userId);
            }

            if (scopeExpression != null) {
                return where != null ? new AndExpression(where, scopeExpression) : scopeExpression;
            }

            return where;
        } catch (Exception e) {
            log.warn("Data permission handler error: {}", e.getMessage());
            return where;
        }
    }

    private Expression buildUserScope(Table table, Long userId) {
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, "user_id"));
        equalsTo.setRightExpression(new LongValue(userId));
        return equalsTo;
    }

    private Expression buildMerchantScope(Table table, Long userId) {
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, "merchant_id"));
        equalsTo.setRightExpression(new LongValue(userId));
        return equalsTo;
    }

    /**
     * 获取当前操作的表对象。
     * 优先通过 MyBatis Plus 的 TableInfoHelper 从 mappedStatementId 反查表名，
     * 如果无法识别，则尝试从 DataPermissionContext（ThreadLocal）获取。
     *
     * @param mappedStatementId MyBatis MappedStatement ID，格式如 com.xxx.mapper.ProductMapper.selectPage
     * @return Table 对象，如果无法识别表名则返回 null
     */
    private Table getSelectTable(String mappedStatementId) {
        // 方案一：通过 mappedStatementId 匹配 TableInfo 获取表名
        if (mappedStatementId != null) {
            for (TableInfo tableInfo : TableInfoHelper.getTableInfos()) {
                String namespace = tableInfo.getCurrentNamespace();
                if (namespace != null && mappedStatementId.startsWith(namespace + ".")) {
                    Table table = new Table();
                    table.setName(tableInfo.getTableName());
                    log.debug("数据权限 - 通过 mappedStatementId 识别到表: {}", tableInfo.getTableName());
                    return table;
                }
            }
        }

        // 方案二：从 ThreadLocal 上下文获取表名（兜底机制）
        String contextTableName = DataPermissionContext.getCurrentTable();
        if (contextTableName != null) {
            Table table = new Table();
            table.setName(contextTableName);
            log.debug("数据权限 - 通过 ThreadLocal 上下文获取到表: {}", contextTableName);
            return table;
        }

        log.debug("数据权限 - 无法识别当前操作的表名，mappedStatementId: {}", mappedStatementId);
        return null;
    }
}