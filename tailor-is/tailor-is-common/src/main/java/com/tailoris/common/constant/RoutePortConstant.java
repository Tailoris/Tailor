package com.tailoris.common.constant;

public final class RoutePortConstant {

    private RoutePortConstant() {
    }

    public static final String API_BASE_PREFIX = "/api";
    public static final String API_VERSION_V1 = "/v1";
    public static final String API_V1_PREFIX = API_BASE_PREFIX + API_VERSION_V1;

    public static final String ROUTE_USER = API_V1_PREFIX + "/user";
    public static final String ROUTE_AUTH = API_V1_PREFIX + "/auth";
    public static final String ROUTE_COPYRIGHT = API_V1_PREFIX + "/copyright";
    public static final String ROUTE_PATTERN = API_V1_PREFIX + "/pattern";
    public static final String ROUTE_DESIGN = API_V1_PREFIX + "/design";
    public static final String ROUTE_FABRIC = API_V1_PREFIX + "/fabric";
    public static final String ROUTE_SUPPLY = API_V1_PREFIX + "/supply";
    public static final String ROUTE_ORDER = API_V1_PREFIX + "/order";
    public static final String ROUTE_TRADE = API_V1_PREFIX + "/trade";
    public static final String ROUTE_DATA = API_V1_PREFIX + "/data";
    public static final String ROUTE_COMMUNITY = API_V1_PREFIX + "/community";
    public static final String ROUTE_EDU = API_V1_PREFIX + "/edu";
    public static final String ROUTE_AI = API_V1_PREFIX + "/ai";
    public static final String ROUTE_ADMIN = API_V1_PREFIX + "/admin";
    public static final String ROUTE_ADMIN_DASHBOARD = ROUTE_ADMIN + "/dashboard";
    public static final String ROUTE_ADMIN_USERS = ROUTE_ADMIN + "/users";
    public static final String ROUTE_ADMIN_ROLES = ROUTE_ADMIN + "/roles";
    public static final String ROUTE_ADMIN_ORDER = ROUTE_ADMIN + "/order";
    public static final String ROUTE_ADMIN_PRODUCT = ROUTE_ADMIN + "/product";
    public static final String ROUTE_ADMIN_MERCHANT = ROUTE_ADMIN + "/merchant";
    public static final String ROUTE_ADMIN_COMMUNITY = ROUTE_ADMIN + "/community";
    public static final String ROUTE_ADMIN_COPYRIGHT = ROUTE_ADMIN + "/copyright";
    public static final String ROUTE_ADMIN_PATTERN = ROUTE_ADMIN + "/pattern";
    public static final String ROUTE_ADMIN_SUPPLY = ROUTE_ADMIN + "/supply";
    public static final String ROUTE_ADMIN_MARKETING = ROUTE_ADMIN + "/marketing";
    public static final String ROUTE_ADMIN_ANALYTICS = ROUTE_ADMIN + "/analytics";
    public static final String ROUTE_ADMIN_ACADEMY = ROUTE_ADMIN + "/academy";
    public static final String ROUTE_ADMIN_AUDIT_LOGS = ROUTE_ADMIN + "/audit-logs";
    public static final String ROUTE_ADMIN_SYSTEM_SETTINGS = ROUTE_ADMIN + "/system/settings";

    public static final String ROUTE_PRODUCT = API_V1_PREFIX + "/product";
    public static final String ROUTE_PRODUCTS = API_V1_PREFIX + "/products";
    public static final String ROUTE_PRODUCT_CATEGORY = ROUTE_PRODUCT + "/category";
    public static final String ROUTE_FAVORITE = API_V1_PREFIX + "/favorite";

    public static final String ROUTE_MERCHANT = API_V1_PREFIX + "/merchant";
    public static final String ROUTE_MERCHANT_DASHBOARD = ROUTE_MERCHANT + "/dashboard";
    public static final String ROUTE_MERCHANT_ROLE = ROUTE_MERCHANT + "/role";
    public static final String ROUTE_MERCHANT_VIOLATION = ROUTE_MERCHANT + "/violation";
    public static final String ROUTE_MERCHANT_TRIAL = ROUTE_MERCHANT + "/trial";
    public static final String ROUTE_MERCHANT_NOTICES = ROUTE_MERCHANT + "/notices";
    public static final String ROUTE_MERCHANT_SHOP_SWITCH = ROUTE_MERCHANT + "/shop-switch";

    public static final String ROUTE_MARKETING = API_V1_PREFIX + "/marketing";
    public static final String ROUTE_MARKETING_COUPON = ROUTE_MARKETING + "/coupon";
    public static final String ROUTE_MARKETING_POINTS = ROUTE_MARKETING + "/points";
    public static final String ROUTE_MARKETING_SECKILL = ROUTE_MARKETING + "/seckill";

    public static final String ROUTE_MESSAGE = API_V1_PREFIX + "/message";
    public static final String ROUTE_PAYMENT = API_V1_PREFIX + "/payment";
    public static final String ROUTE_PAYMENT_ESCROW = ROUTE_PAYMENT + "/escrow";
    public static final String ROUTE_PAYMENT_RECONCILIATION = ROUTE_PAYMENT + "/reconciliation";
    public static final String ROUTE_SETTLEMENT = API_V1_PREFIX + "/settlement";
    public static final String ROUTE_ACCOUNT = API_V1_PREFIX + "/account";
    public static final String ROUTE_SANDBOX = API_V1_PREFIX + "/sandbox";

    public static final String ROUTE_ANALYTICS = API_V1_PREFIX + "/analytics";
    public static final String ROUTE_ACADEMY = API_V1_PREFIX + "/academy";

    public static final String ROUTE_USER_ROLES = ROUTE_USER + "/roles";
    public static final String ROUTE_USER_ADDRESS = ROUTE_USER + "/address";

    public static final String ROUTE_COMMUNITY_POST = ROUTE_COMMUNITY + "/post";
    public static final String ROUTE_COMMUNITY_COMMENT = ROUTE_COMMUNITY + "/comment";

    public static final String ROUTE_AI_SIZE = ROUTE_AI + "/size";
    public static final String ROUTE_AI_PATTERN = ROUTE_AI + "/pattern";

    public static final String ROUTE_CART = API_V1_PREFIX + "/cart";
    public static final String ROUTE_ORDER_AFTER_SALE = ROUTE_ORDER + "/after-sale";

    public static final String ROUTE_PRODUCT_REVIEW = ROUTE_PRODUCT + "/review";

    public static final String ROUTE_COPYRIGHT_INFRINGEMENT = ROUTE_COPYRIGHT + "/infringement";

    public static final String ROUTE_KNOWLEDGE = API_V1_PREFIX + "/knowledge";
    public static final String ROUTE_IM = API_V1_PREFIX + "/im";

    // ---- 端口常量（v4.0 对齐 docker-compose.services.yml 实际部署端口） ----
    // 端口分配策略（v4.0 最终版）：
    //   核心业务服务: 8100-8107 (admin, user, product, order, payment, marketing, ai, copyright)
    //   轻量业务服务: 8108-8117 (community, supply, merchant, message, academy, analytics, im, pattern, design, knowledge)
    // 注意：此端口分配已与 .trae/rules/micro-service-route-port-rule.md 对齐更新
    // 核心业务端口段 (8100-8107)
    public static final int PORT_ADMIN_SERVICE = 8100;
    public static final int PORT_USER_SERVICE = 8101;
    public static final int PORT_PRODUCT_SERVICE = 8102;
    public static final int PORT_ORDER_SERVICE = 8103;
    public static final int PORT_PAYMENT_SERVICE = 8104;
    public static final int PORT_MARKETING_SERVICE = 8105;
    public static final int PORT_AI_SERVICE = 8106;
    public static final int PORT_COPYRIGHT_SERVICE = 8107;
    // 轻量业务端口段 (8108-8117)
    public static final int PORT_COMMUNITY_SERVICE = 8108;
    public static final int PORT_SUPPLY_SERVICE = 8109;
    public static final int PORT_MERCHANT_SERVICE = 8110;
    public static final int PORT_MESSAGE_SERVICE = 8111;
    public static final int PORT_ACADEMY_SERVICE = 8112;
    public static final int PORT_ANALYTICS_SERVICE = 8113;
    public static final int PORT_IM_SERVICE = 8114;
    public static final int PORT_PATTERN_SERVICE = 8115;
    public static final int PORT_DESIGN_SERVICE = 8116;
    public static final int PORT_KNOWLEDGE_SERVICE = 8117;
    // 网关端口段
    public static final int PORT_CORE_GATEWAY = 9000;
    public static final int PORT_LITE_GATEWAY = 8081;

    public static final int PORT_REDIS = 6379;
    public static final int PORT_MONGODB = 27017;
    public static final int PORT_MYSQL = 3306;
    public static final int PORT_NGINX_HTTP = 80;
    public static final int PORT_NGINX_HTTPS = 443;
}