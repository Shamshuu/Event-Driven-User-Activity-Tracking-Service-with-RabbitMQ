CREATE DATABASE IF NOT EXISTS user_activity_db;

USE user_activity_db;

CREATE TABLE IF NOT EXISTS user_activities (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL,
    event_type  VARCHAR(50)  NOT NULL,
    timestamp   DATETIME     NOT NULL,
    metadata    JSON,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_event_type (event_type),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
