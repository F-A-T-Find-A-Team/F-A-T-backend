-- Reference schema for empty databases, based on 043c352.
-- Existing databases require reviewed, explicit baseline at version 1.
CREATE TABLE `user` (
 user_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_password VARCHAR(255) NOT NULL,
 user_email VARCHAR(255) NOT NULL,
 user_gender VARCHAR(255) NOT NULL,
 user_major VARCHAR(255) NOT NULL,
 user_student_number INT NOT NULL,
 user_name VARCHAR(255) NOT NULL,
 user_bio VARCHAR(100), club_name VARCHAR(255), github_username VARCHAR(255),
 CONSTRAINT uk_user_email UNIQUE (user_email)
) ENGINE=InnoDB;
CREATE TABLE project (
 project_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 pm_id BIGINT NOT NULL,
 project_title VARCHAR(255) NOT NULL,
 project_description TEXT NOT NULL,
 project_deadline DATE NOT NULL,
 project_status ENUM('IDEA','IN_PROGRESS','COMPLETED') NOT NULL,
 view_count INT NOT NULL,
 recruit_limit INT NOT NULL,
 CONSTRAINT fk_project_pm FOREIGN KEY (pm_id) REFERENCES `user`(user_id)
) ENGINE=InnoDB;
CREATE TABLE user_interest_stack (
 user_id BIGINT NOT NULL, interest_stack VARCHAR(255),
 CONSTRAINT fk_interest_user FOREIGN KEY (user_id) REFERENCES `user`(user_id)
) ENGINE=InnoDB;
CREATE TABLE project_required_major (
 project_id BIGINT NOT NULL, required_major VARCHAR(255),
 CONSTRAINT fk_major_project FOREIGN KEY (project_id) REFERENCES project(project_id)
) ENGINE=InnoDB;
CREATE TABLE project_required_stack (
 project_id BIGINT NOT NULL, required_stack VARCHAR(255),
 CONSTRAINT fk_stack_project FOREIGN KEY (project_id) REFERENCES project(project_id)
) ENGINE=InnoDB;
CREATE TABLE project_application (
 application_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 project_id BIGINT NOT NULL, applicant_id BIGINT NOT NULL,
 application_message VARCHAR(255),
 application_status ENUM('PENDING','ACCEPTED','REJECTED') NOT NULL,
 CONSTRAINT fk_application_project FOREIGN KEY (project_id) REFERENCES project(project_id),
 CONSTRAINT fk_application_user FOREIGN KEY (applicant_id) REFERENCES `user`(user_id)
) ENGINE=InnoDB;
CREATE TABLE chat_room (
 chat_room_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 project_id BIGINT NOT NULL,
 CONSTRAINT uk_room_project UNIQUE (project_id),
 CONSTRAINT fk_room_project FOREIGN KEY (project_id) REFERENCES project(project_id)
) ENGINE=InnoDB;
CREATE TABLE chat_message (
 chat_message_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 chat_room_id BIGINT NOT NULL, sender_id BIGINT NOT NULL,
 message_content TEXT NOT NULL, sent_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_message_room FOREIGN KEY (chat_room_id) REFERENCES chat_room(chat_room_id),
 CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES `user`(user_id)
) ENGINE=InnoDB;
CREATE TABLE feedback (
 feedback_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 project_id BIGINT NOT NULL, writer_id BIGINT NOT NULL, target_id BIGINT NOT NULL,
 rating INT NOT NULL, feedback_content TEXT,
 CONSTRAINT fk_feedback_project FOREIGN KEY (project_id) REFERENCES project(project_id),
 CONSTRAINT fk_feedback_writer FOREIGN KEY (writer_id) REFERENCES `user`(user_id),
 CONSTRAINT fk_feedback_target FOREIGN KEY (target_id) REFERENCES `user`(user_id)
) ENGINE=InnoDB;
CREATE TABLE inquiry (
 inquiry_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 project_id BIGINT NOT NULL, applicant_id BIGINT NOT NULL,
 CONSTRAINT fk_inquiry_project FOREIGN KEY (project_id) REFERENCES project(project_id),
 CONSTRAINT fk_inquiry_applicant FOREIGN KEY (applicant_id) REFERENCES `user`(user_id)
) ENGINE=InnoDB;
