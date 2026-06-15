package com.tailoris.common.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.tailoris.common.constant.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
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
            "sys_user", "order_info", "product", "merchant", "community_post",
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

            Table table = getSelectTable(where);

            Expression scopeExpression = null;

            if (CommonConstants.USER_TYPE_MERCHANT.equals(userType) && table != null) {
                scopeExpression = buildMerchantScope(table, userId);
            }

            if (table != null && DATA_SCOPE_TABLES.contains(table.getName().toLowerCase())) {
                Expression userScope = buildUserScope(table, userId);
                scopeExpression = scopeExpression != null
                        ? new AndExpression(scopeExpression, userScope)
                        : userScope;
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

    private Table getSelectTable(Expression where) {
        return null;
    }
}