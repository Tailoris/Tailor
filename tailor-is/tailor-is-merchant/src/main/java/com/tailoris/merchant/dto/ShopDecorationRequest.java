package com.tailoris.merchant.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 店铺装修配置请求 - MER-004.
 *
 * <p>支持基础版装修：</p>
 * <ul>
 *   <li>店铺Logo/Banner</li>
 *   <li>店铺主题色</li>
 *   <li>公告</li>
 *   <li>导航菜单（首页/分类/活动/会员）</li>
 *   <li>首页布局（轮播图/商品分类/推荐商品）</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Data
public class ShopDecorationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "店铺ID不能为空")
    private Long shopId;

    /** 店铺Logo URL */
    @Size(max = 512, message = "Logo URL过长")
    private String shopLogo;

    /** 店铺Banner URL */
    @Size(max = 512, message = "Banner URL过长")
    private String shopBanner;

    /** 店铺描述 */
    @Size(max = 1000, message = "店铺描述过长")
    private String shopDesc;

    /** 主题色 #RRGGBB */
    @Size(max = 7, message = "主题色格式错误")
    private String shopTheme;

    /** 公告 */
    @Size(max = 500, message = "公告过长")
    private String announcement;

    /** 客服联系方式 */
    @Size(max = 200)
    private String contactService;

    /** 装修配置JSON */
    private String decorationConfig;

    /** 导航菜单 */
    private List<NavItem> navItems;

    /** 首页模块 */
    private List<HomeModule> homeModules;

    @Data
    public static class NavItem implements Serializable {
        private String name;
        private String icon;
        private String link;
        private Integer sortOrder;
    }

    @Data
    public static class HomeModule implements Serializable {
        private String type;     // banner/product/recommend/category
        private String title;
        private Integer sortOrder;
        private Object config;   // 模块配置
    }
}
