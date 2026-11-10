-- Preserve existing names and student numbers; new identity fields are additive.
-- Do not infer verified school-email ownership for legacy accounts.
ALTER TABLE users
 ADD COLUMN nickname VARCHAR(40),
 ADD COLUMN real_name VARCHAR(255),
 ADD COLUMN student_id VARCHAR(64),
 ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
 ADD CONSTRAINT uk_users_nickname UNIQUE (nickname);
UPDATE users SET real_name = user_name, student_id = CAST(user_student_number AS CHAR);
CREATE TABLE user_majors (
 user_id BIGINT NOT NULL,
 major VARCHAR(255) NOT NULL,
 PRIMARY KEY (user_id, major),
 CONSTRAINT fk_user_major_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;
INSERT INTO user_majors (user_id, major) SELECT user_id, user_major FROM users;
