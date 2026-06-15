-- ======================================================================
-- Tailor IS 数据库初始化脚本 - 1Panel 生产环境
-- ======================================================================
-- 版本: v1.0
-- 执行方式:
--   1. 启动 docker-compose 时自动挂载到 /docker-entrypoint-initdb.d
--   2. 或手动: mysql -u root -p < 000_create_users.sql
-- ======================================================================

-- 1. 创建应用数据库
CREATE DATABASE IF NOT EXISTS tailor_is
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 2. 创建 Nacos 配置库 (Nacos 3.x 必需)
CREATE DATABASE IF NOT EXISTS nacos_config
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 3. 创建应用用户 (最低权限原则)
--    注意: 1Panel 部署时请通过 .env 配置以下密码
CREATE USER IF NOT EXISTS 'tailor_is_app'@'%' IDENTIFIED BY 'change-me-in-env-file';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, INDEX, ALTER ON tailor_is.* TO 'tailor_is_app'@'%';

-- 4. Nacos 用户
CREATE USER IF NOT EXISTS 'nacos'@'%' IDENTIFIED BY 'change-me-in-env-file';
GRANT ALL PRIVILEGES ON nacos_config.* TO 'nacos'@'%';

-- 5. 只读监控用户 (可选)
CREATE USER IF NOT EXISTS 'tailor_is_monitor'@'%' IDENTIFIED BY 'change-me-in-env-file';
GRANT SELECT ON tailor_is.* TO 'tailor_is_monitor'@'%';
GRANT PROCESS ON *.* TO 'tailor_is_monitor'@'%';

FLUSH PRIVILEGES;

-- ======================================================================
-- 6. 核心业务表 (示例 - 完整表结构由 Flyway/Liquibase 管理)
-- ======================================================================
USE tailor_is;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(128) UNIQUE COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt哈希密码',
    avatar_url VARCHAR(512) COMMENT '头像URL',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-正常, 0-禁用',
    failed_login_count INT DEFAULT 0 COMMENT '失败登录次数',
    locked_until DATETIME COMMENT '锁定截止时间',
    last_login_at DATETIME COMMENT '最后登录时间',
    last_login_ip VARCHAR(64) COMMENT '最后登录IP',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 商品表
CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL COMMENT '商户ID',
    name VARCHAR(255) NOT NULL COMMENT '商品名称',
    description TEXT COMMENT '商品描述',
    price DECIMAL(12,2) NOT NULL COMMENT '价格',
    stock INT DEFAULT 0 COMMENT '库存',
    category_id BIGINT COMMENT '分类ID',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-上架, 0-下架',
    sales_count INT DEFAULT 0 COMMENT '销量',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_merchant (merchant_id),
    INDEX idx_category (category_id),
    INDEX idx_status (status),
    FULLTEXT INDEX idx_name_desc (name, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    merchant_id BIGINT NOT NULL COMMENT '商户ID',
    total_amount DECIMAL(12,2) NOT NULL COMMENT '订单总金额',
    pay_amount DECIMAL(12,2) COMMENT '实付金额',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态',
    pay_method VARCHAR(32) COMMENT '支付方式: WECHAT/ALIPAY',
    paid_at DATETIME COMMENT '支付时间',
    shipped_at DATETIME COMMENT '发货时间',
    delivered_at DATETIME COMMENT '送达时间',
    cancelled_at DATETIME COMMENT '取消时间',
    cancel_reason VARCHAR(255) COMMENT '取消原因',
    shipping_address VARCHAR(512) COMMENT '收货地址',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_no (order_no),
    INDEX idx_user (user_id),
    INDEX idx_merchant (merchant_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 订单明细表
CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(255) NOT NULL COMMENT '商品名称快照',
    price DECIMAL(12,2) NOT NULL COMMENT '下单时单价',
    quantity INT NOT NULL COMMENT '数量',
    subtotal DECIMAL(12,2) NOT NULL COMMENT '小计',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order (order_id),
    INDEX idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';

-- 支付记录表
CREATE TABLE IF NOT EXISTS payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_no VARCHAR(64) NOT NULL UNIQUE COMMENT '支付流水号',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    user_id BIGINT NOT NULL,
    pay_method VARCHAR(32) NOT NULL COMMENT '支付方式',
    pay_channel VARCHAR(32) COMMENT '支付渠道',
    amount DECIMAL(12,2) NOT NULL COMMENT '支付金额',
    currency VARCHAR(8) DEFAULT 'CNY',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '支付状态',
    third_party_no VARCHAR(255) COMMENT '第三方支付流水号',
    notify_body TEXT COMMENT '回调原始数据',
    paid_at DATETIME COMMENT '完成时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_payment_no (payment_no),
    INDEX idx_order (order_id),
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- 商户表
CREATE TABLE IF NOT EXISTS merchant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '关联用户',
    shop_name VARCHAR(128) NOT NULL COMMENT '店铺名称',
    shop_logo VARCHAR(512) COMMENT '店铺LOGO',
    description TEXT COMMENT '店铺描述',
    status VARCHAR(32) DEFAULT 'PENDING' COMMENT '状态: PENDING/ACTIVE/SUSPENDED',
    rating DECIMAL(3,2) DEFAULT 5.00 COMMENT '评分',
    total_orders INT DEFAULT 0,
    verified_at DATETIME COMMENT '认证时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户表';

-- 社区帖子表
CREATE TABLE IF NOT EXISTS post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    author_id BIGINT NOT NULL COMMENT '作者ID',
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    cover_image VARCHAR(512) COMMENT '封面图',
    category VARCHAR(64) COMMENT '分类',
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    is_published TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_author (author_id),
    INDEX idx_category (category),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区帖子表';

-- 操作日志表 (审计)
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT '操作人',
    action VARCHAR(64) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(64) COMMENT '目标类型',
    target_id BIGINT COMMENT '目标ID',
    ip_address VARCHAR(64) COMMENT 'IP地址',
    user_agent VARCHAR(512) COMMENT '浏览器UA',
    request_method VARCHAR(16),
    request_path VARCHAR(512),
    request_params TEXT,
    response_status INT,
    duration_ms INT COMMENT '耗时(ms)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_action (action),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- ======================================================================
-- 7. 初始化数据
-- ======================================================================
-- 管理员账户 (密码: Admin@123, 使用时请用 BCrypt 重新生成)
INSERT INTO sys_user (username, email, password_hash, status) VALUES
    ('admin', 'admin@tailoris.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.zX1yPfTjNq5M2tRc3z', 1)
    ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 测试商品 (开发环境)
INSERT INTO product (merchant_id, name, description, price, stock, category_id, status) VALUES
    (1, '经典男式衬衫', '高品质纯棉面料，多种颜色可选', 199.00, 100, 1, 1),
    (1, '女式连衣裙', '优雅设计，适合多种场合', 299.00, 50, 2, 1)
    ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;
