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
