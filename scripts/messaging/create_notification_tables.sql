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
