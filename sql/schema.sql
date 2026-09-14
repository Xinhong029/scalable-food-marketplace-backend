-- LastCall Eats Database Schema
-- MySQL 8.x
-- 执行前请先创建数据库：CREATE DATABASE lastcall_eats;

CREATE DATABASE IF NOT EXISTS lastcall_eats
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE lastcall_eats;

-- =====================
-- 用户表
-- =====================
CREATE TABLE IF NOT EXISTS `user` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `email`         VARCHAR(255)    NOT NULL,
    `password_hash` VARCHAR(255)    NOT NULL,
    `nickname`      VARCHAR(100)    NOT NULL,
    `avatar_url`    VARCHAR(500)    DEFAULT NULL,
    `is_active`     TINYINT(1)      NOT NULL DEFAULT 1,
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- 商家表
-- =====================
CREATE TABLE IF NOT EXISTS `merchant` (
    `id`             BIGINT          NOT NULL AUTO_INCREMENT,
    `email`          VARCHAR(255)    NOT NULL,
    `password_hash`  VARCHAR(255)    NOT NULL,
    `name`           VARCHAR(255)    NOT NULL,
    `address`        VARCHAR(500)    NOT NULL,
    `business_hours` VARCHAR(255)    DEFAULT NULL,
    `is_active`      TINYINT(1)      NOT NULL DEFAULT 1,
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_merchant_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- 商品模板表
-- =====================
CREATE TABLE IF NOT EXISTS `product_template` (
    `id`             BIGINT          NOT NULL AUTO_INCREMENT,
    `merchant_id`    BIGINT          NOT NULL,
    `name`           VARCHAR(255)    NOT NULL,
    `description`    TEXT            DEFAULT NULL,
    `original_price` DECIMAL(10, 2)  NOT NULL,
    `is_active`      TINYINT(1)      NOT NULL DEFAULT 1,
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_template_merchant` (`merchant_id`),
    CONSTRAINT `fk_template_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- 限时商品表
-- =====================
CREATE TABLE IF NOT EXISTS `product_listing` (
    `id`                 BIGINT          NOT NULL AUTO_INCREMENT,
    `merchant_id`        BIGINT          NOT NULL,
    `template_id`        BIGINT          NOT NULL,
    `discount_price`     DECIMAL(10, 2)  NOT NULL,
    `quantity`           INT             NOT NULL,
    `remaining_quantity` INT             NOT NULL,
    `pickup_start`       TIME            NOT NULL,
    `pickup_end`         TIME            NOT NULL,
    `date`               DATE            NOT NULL,
    `is_available`       TINYINT(1)      NOT NULL DEFAULT 1,
    `created_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_listing_merchant` (`merchant_id`),
    KEY `idx_listing_template` (`template_id`),
    KEY `idx_listing_date` (`date`),
    KEY `idx_listing_browse` (`is_available`, `created_at` DESC, `id` DESC),
    CONSTRAINT `fk_listing_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`),
    CONSTRAINT `fk_listing_template` FOREIGN KEY (`template_id`) REFERENCES `product_template` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- 用户收藏表
-- =====================
CREATE TABLE IF NOT EXISTS `user_favorite` (
    `id`          BIGINT    NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT    NOT NULL,
    `listing_id`  BIGINT    NOT NULL,
    `created_at`  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_favorite` (`user_id`, `listing_id`),
    KEY `idx_user_favorite_user` (`user_id`),
    KEY `idx_user_favorite_listing` (`listing_id`),
    CONSTRAINT `fk_user_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_user_favorite_listing` FOREIGN KEY (`listing_id`) REFERENCES `product_listing` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- 订单表
-- =====================
CREATE TABLE IF NOT EXISTS `orders` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT,
    `user_id`           BIGINT          NOT NULL,
    `listing_id`        BIGINT          NOT NULL,
    `merchant_id`       BIGINT          NOT NULL,
    `price`             DECIMAL(10, 2)  NOT NULL,
    `status`            VARCHAR(20)     NOT NULL,
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_user` (`user_id`),
    KEY `idx_order_merchant` (`merchant_id`),
    KEY `idx_order_listing` (`listing_id`),
    CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_order_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`),
    CONSTRAINT `fk_order_listing` FOREIGN KEY (`listing_id`) REFERENCES `product_listing` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- 事务发件箱表
-- =====================
CREATE TABLE IF NOT EXISTS `outbox_event` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `event_id`       VARCHAR(36)   NOT NULL,
    `aggregate_type` VARCHAR(50)   NOT NULL,
    `aggregate_id`   BIGINT        NOT NULL,
    `event_type`     VARCHAR(100)  NOT NULL,
    `exchange_name`  VARCHAR(255)  NOT NULL,
    `routing_key`    VARCHAR(255)  NOT NULL,
    `payload`        TEXT          NOT NULL,
    `status`         VARCHAR(20)   NOT NULL,
    `retry_count`    INT           NOT NULL DEFAULT 0,
    `next_retry_at`  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `locked_at`      DATETIME(6)   DEFAULT NULL,
    `published_at`   DATETIME(6)   DEFAULT NULL,
    `last_error`     VARCHAR(1000) DEFAULT NULL,
    `created_at`     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                      ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_outbox_event_id` (`event_id`),
    KEY `idx_outbox_ready` (`status`, `next_retry_at`, `created_at`, `id`),
    KEY `idx_outbox_aggregate` (`aggregate_type`, `aggregate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- 商家通知与消费幂等记录
-- =====================
CREATE TABLE IF NOT EXISTS `merchant_notification` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT,
    `merchant_id`       BIGINT        NOT NULL,
    `order_id`          BIGINT        NOT NULL,
    `event_id`          VARCHAR(36)   NOT NULL,
    `notification_type` VARCHAR(100)  NOT NULL,
    `title`             VARCHAR(255)  NOT NULL,
    `content`           VARCHAR(1000) NOT NULL,
    `is_read`           TINYINT(1)    NOT NULL DEFAULT 0,
    `read_at`           DATETIME(6)   DEFAULT NULL,
    `created_at`        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notification_event` (`event_id`),
    KEY `idx_notification_merchant` (`merchant_id`, `is_read`, `created_at` DESC),
    KEY `idx_notification_order` (`order_id`),
    CONSTRAINT `fk_notification_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`),
    CONSTRAINT `fk_notification_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `processed_event` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `event_id`      VARCHAR(36)  NOT NULL,
    `consumer_name` VARCHAR(100) NOT NULL,
    `processed_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_processed_event_consumer` (`event_id`, `consumer_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- 取货码表
-- =====================
CREATE TABLE IF NOT EXISTS `pickup_code` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `order_id`     BIGINT       NOT NULL,
    `numeric_code` VARCHAR(6)   NOT NULL,
    `qr_code`      TEXT         DEFAULT NULL,
    `used`         TINYINT(1)   NOT NULL DEFAULT 0,
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`),
    CONSTRAINT `fk_pickup_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- 评价表
-- =====================
CREATE TABLE IF NOT EXISTS `review` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT,
    `order_id`    BIGINT          NOT NULL,
    `user_id`     BIGINT          NOT NULL,
    `merchant_id` BIGINT          NOT NULL,
    `template_id` BIGINT          NOT NULL,
    `rating`      INT             NOT NULL,
    `content`     TEXT            DEFAULT NULL,
    `image_urls`  JSON            DEFAULT NULL,
    `is_visible`  TINYINT(1)      NOT NULL DEFAULT 1,
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_review_order` (`order_id`),
    KEY `idx_review_user` (`user_id`),
    KEY `idx_review_merchant` (`merchant_id`),
    KEY `idx_review_template` (`template_id`),
    CONSTRAINT `fk_review_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
    CONSTRAINT `fk_review_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_review_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`),
    CONSTRAINT `fk_review_template` FOREIGN KEY (`template_id`) REFERENCES `product_template` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- 社区帖子表
-- =====================
CREATE TABLE IF NOT EXISTS `post` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT      NOT NULL,
    `merchant_id`   BIGINT      DEFAULT NULL,
    `content`       TEXT        NOT NULL,
    `image_urls`    JSON        DEFAULT NULL,
    `like_count`    INT         NOT NULL DEFAULT 0,
    `comment_count` INT         NOT NULL DEFAULT 0,
    `is_visible`    TINYINT(1)  NOT NULL DEFAULT 1,
    `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_post_user` (`user_id`),
    KEY `idx_post_merchant` (`merchant_id`),
    CONSTRAINT `fk_post_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_post_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
