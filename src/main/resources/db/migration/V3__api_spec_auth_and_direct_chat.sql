-- Additive changes for the supplied Notion API catalogue (GitHub excluded).
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(512);
ALTER TABLE projects ADD COLUMN recruiting_open BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE projects SET recruiting_open=FALSE WHERE project_status='COMPLETED';
ALTER TABLE applications ADD COLUMN application_position VARCHAR(255),
 MODIFY application_status ENUM('PENDING','ACCEPTED','REJECTED','CANCELLED') NOT NULL;

-- Authentication state stores digests, never raw codes, signup tokens or refresh tokens.
CREATE TABLE email_verifications (
 verification_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 email VARCHAR(255) NOT NULL,
 code_hash CHAR(64) NOT NULL,
 code_expires_at DATETIME(6) NOT NULL,
 attempts INT NOT NULL DEFAULT 0,
 verified_at DATETIME(6),
 signup_token_hash CHAR(64),
 signup_token_expires_at DATETIME(6),
 consumed_at DATETIME(6),
 CONSTRAINT uk_verification_signup_token UNIQUE (signup_token_hash),
 CONSTRAINT ck_verification_attempts CHECK (attempts BETWEEN 0 AND 5),
 CONSTRAINT ck_verification_signup_pair CHECK (
  (signup_token_hash IS NULL AND signup_token_expires_at IS NULL) OR
  (signup_token_hash IS NOT NULL AND signup_token_expires_at IS NOT NULL AND verified_at IS NOT NULL)),
 INDEX ix_verification_email_expiry (email, code_expires_at)
) ENGINE=InnoDB;
CREATE TABLE refresh_tokens (
 refresh_token_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 token_hash CHAR(64) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 expires_at DATETIME(6) NOT NULL,
 revoked_at DATETIME(6),
 CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
 CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(user_id),
 CONSTRAINT ck_refresh_token_expiry CHECK (expires_at > created_at),
 INDEX ix_refresh_user_expiry (user_id, expires_at)
) ENGINE=InnoDB;

ALTER TABLE chat_rooms
 ADD COLUMN room_type ENUM('TEAM','DIRECT') NOT NULL DEFAULT 'TEAM',
 ADD COLUMN requester_id BIGINT,
 ADD COLUMN team_project_id BIGINT GENERATED ALWAYS AS (CASE WHEN room_type='TEAM' THEN project_id ELSE NULL END) STORED,
 ADD CONSTRAINT uk_team_room UNIQUE (team_project_id),
 ADD CONSTRAINT uk_direct_room UNIQUE (project_id, requester_id),
 ADD CONSTRAINT fk_direct_requester FOREIGN KEY (requester_id) REFERENCES users(user_id),
 ADD CONSTRAINT ck_room_type CHECK ((room_type='TEAM' AND requester_id IS NULL) OR (room_type='DIRECT' AND requester_id IS NOT NULL)),
 ADD INDEX ix_room_project (project_id);
-- Hibernate-generated legacy UNIQUE names differ by installation. Discover the
-- single-column project_id UNIQUE index instead of assuming its generated name.
DROP PROCEDURE IF EXISTS fat_remove_legacy_room_unique;
DELIMITER //
CREATE PROCEDURE fat_remove_legacy_room_unique()
BEGIN
 SELECT GROUP_CONCAT(CONCAT('DROP INDEX `', REPLACE(index_name,'`','``'), '`') SEPARATOR ', ')
 INTO @fat_drop_indexes
 FROM (
  SELECT index_name FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='chat_rooms' AND non_unique=0 AND index_name<>'PRIMARY'
  GROUP BY index_name HAVING COUNT(*)=1 AND MIN(column_name)='project_id'
 ) legacy_indexes;
 IF @fat_drop_indexes IS NOT NULL THEN
  SET @fat_ddl=CONCAT('ALTER TABLE chat_rooms ',@fat_drop_indexes);
  PREPARE fat_stmt FROM @fat_ddl;
  EXECUTE fat_stmt;
  DEALLOCATE PREPARE fat_stmt;
 END IF;
END //
DELIMITER ;
CALL fat_remove_legacy_room_unique();
DROP PROCEDURE fat_remove_legacy_room_unique;

ALTER TABLE chat_messages ADD CONSTRAINT uk_message_room_id UNIQUE (chat_room_id, chat_message_id);
ALTER TABLE chat_participants ADD COLUMN last_read_message_id BIGINT,
 ADD CONSTRAINT fk_participant_read_message FOREIGN KEY (chat_room_id, last_read_message_id)
 REFERENCES chat_messages(chat_room_id, chat_message_id);
