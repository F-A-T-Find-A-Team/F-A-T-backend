-- Additive API state. Original creation times are unknown; timestamps below are import times.
ALTER TABLE projects ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 ADD COLUMN completed_at DATETIME(6) NULL;
UPDATE projects SET completed_at=CURRENT_TIMESTAMP(6) WHERE project_status='COMPLETED';
ALTER TABLE applications ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE feedback ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE chat_messages ADD COLUMN client_message_id VARCHAR(128) NULL,
 ADD CONSTRAINT uk_message_client UNIQUE(chat_room_id,sender_id,client_message_id);
CREATE TABLE email_delivery_limits (
 email VARCHAR(255) NOT NULL PRIMARY KEY,
 last_sent_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

ALTER TABLE email_verifications ADD COLUMN purpose VARCHAR(32) NOT NULL DEFAULT 'SIGNUP';
