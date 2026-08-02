package com.tailoris.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 仪表盘趋势数据响应。
 *
 * <p>按日维度返回近 N 天的营收与订单趋势，用于前端折线/面积图渲染。</p>
 *
 * @author Tailor IS Team
 */
@Data
@Schema(description = "仪表盘趋势数据响应")
public class DashboardTrendResponse {

    @Schema(description = "趋势数据列表（按日期升序）")
    private List<TrendItem> items;

    @Schema(description = "趋势区间天数")
    private Integer days;

    /**
     * 单日趋势项。
     */
    @Data
    @Schema(description = "单日趋势项")
    public static class TrendItem {

        @Schema(description = "日期 yyyy-MM-dd")
        private String date;

        @Schema(description = "当日营收")
        private BigDecimal amount;

        @Schema(description = "当日订单数")
        private Long orderCount;

        public TrendItem() {
        }

        public TrendItem(String date, BigDecimal amount, Long orderCount) {
            this.date = date;
            this.amount = amount;
            this.orderCount = orderCount;
        }
    }
}
