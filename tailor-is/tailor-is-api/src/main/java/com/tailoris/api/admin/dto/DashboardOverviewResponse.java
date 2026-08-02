package com.tailoris.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 仪表盘概览数据响应。
 *
 * <p>聚合订单状态分布、商家营收排行、商品销量排行，用于前端环形图与排行榜渲染。</p>
 *
 * @author Tailor IS Team
 */
@Data
@Schema(description = "仪表盘概览数据响应")
public class DashboardOverviewResponse {

    @Schema(description = "订单状态分布")
    private List<StatusItem> orderStatusDist;

    @Schema(description = "商家营收排行 TOP10")
    private List<RankItem> topMerchants;

    @Schema(description = "商品销量排行 TOP10")
    private List<RankItem> topProducts;

    /**
     * 订单状态分布项。
     */
    @Data
    @Schema(description = "订单状态分布项")
    public static class StatusItem {

        @Schema(description = "状态码")
        private Integer status;

        @Schema(description = "状态标签")
        private String label;

        @Schema(description = "订单数量")
        private Long count;

        public StatusItem() {
        }

        public StatusItem(Integer status, String label, Long count) {
            this.status = status;
            this.label = label;
            this.count = count;
        }
    }

    /**
     * 排行榜项。
     */
    @Data
    @Schema(description = "排行榜项")
    public static class RankItem {

        @Schema(description = "名称（商家公司名/商品名）")
        private String name;

        @Schema(description = "主指标值（营收金额）")
        private BigDecimal value;

        @Schema(description = "次指标值（订单数/销量数）")
        private Long count;

        public RankItem() {
        }

        public RankItem(String name, BigDecimal value, Long count) {
            this.name = name;
            this.value = value;
            this.count = count;
        }
    }
}
