-- Add Advanced Reporting System
-- Version 15.0.0

-- Create reports table
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    format VARCHAR(20) NOT NULL DEFAULT 'PDF',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    start_date DATE,
    end_date DATE,
    file_path VARCHAR(500),
    file_name VARCHAR(200),
    file_size BIGINT,
    parameters TEXT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_report_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_report_user ON reports(user_id);
CREATE INDEX idx_report_type ON reports(report_type);
CREATE INDEX idx_report_status ON reports(status);

