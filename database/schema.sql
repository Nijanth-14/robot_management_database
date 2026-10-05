-- Requires MySQL 8.0.16 or newer (enforced CHECK constraints).
-- Run once on a fresh database. Existing tables are never dropped.
CREATE DATABASE IF NOT EXISTS robot_monitoring
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE robot_monitoring;

CREATE TABLE robot (
    robot_id VARCHAR(20) NOT NULL,
    robot_name VARCHAR(100) NOT NULL,
    robot_type VARCHAR(50) NOT NULL,
    location VARCHAR(100) NOT NULL,
    status ENUM('Available', 'Busy', 'Charging', 'Offline', 'Maintenance')
        NOT NULL DEFAULT 'Available',
    battery_level TINYINT UNSIGNED NOT NULL DEFAULT 100,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (robot_id),
    CONSTRAINT chk_robot_id CHECK (CHAR_LENGTH(TRIM(robot_id)) > 0),
    CONSTRAINT chk_robot_name CHECK (CHAR_LENGTH(TRIM(robot_name)) > 0),
    CONSTRAINT chk_robot_type CHECK (CHAR_LENGTH(TRIM(robot_type)) > 0),
    CONSTRAINT chk_robot_location CHECK (CHAR_LENGTH(TRIM(location)) > 0),
    CONSTRAINT chk_robot_status CHECK (status IN (
        'Available', 'Busy', 'Charging', 'Offline', 'Maintenance'
    )),
    CONSTRAINT chk_robot_battery CHECK (battery_level BETWEEN 0 AND 100),
    INDEX idx_robot_status (status),
    INDEX idx_robot_location (location)
) ENGINE = InnoDB;

CREATE TABLE task (
    task_id VARCHAR(20) NOT NULL,
    task_name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    priority ENUM('Low', 'Medium', 'High') NOT NULL DEFAULT 'Medium',
    task_status ENUM('Pending', 'In Progress', 'Completed', 'Cancelled')
        NOT NULL DEFAULT 'Pending',
    robot_id VARCHAR(20) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id),
    CONSTRAINT chk_task_id CHECK (CHAR_LENGTH(TRIM(task_id)) > 0),
    CONSTRAINT chk_task_name CHECK (CHAR_LENGTH(TRIM(task_name)) > 0),
    CONSTRAINT chk_task_priority CHECK (priority IN ('Low', 'Medium', 'High')),
    CONSTRAINT chk_task_status CHECK (task_status IN (
        'Pending', 'In Progress', 'Completed', 'Cancelled'
    )),
    CONSTRAINT chk_active_task_assignment CHECK (
        task_status <> 'In Progress' OR robot_id IS NOT NULL
    ),
    CONSTRAINT fk_task_robot FOREIGN KEY (robot_id)
        REFERENCES robot (robot_id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    INDEX idx_task_robot_status (robot_id, task_status),
    INDEX idx_task_status_priority (task_status, priority)
) ENGINE = InnoDB;
