-- Run with application writes stopped. MySQL DDL is not transactionally reversible.
-- No deduplication, deletion, implicit baseline, or FOREIGN_KEY_CHECKS bypass.
DROP PROCEDURE IF EXISTS fat_assert_legacy_data;
DELIMITER //
CREATE PROCEDURE fat_assert_legacy_data()
BEGIN
 IF EXISTS (SELECT 1 FROM project_application GROUP BY project_id, applicant_id HAVING COUNT(*) > 1) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate applications: manual resolution required';
 END IF;
 IF EXISTS (SELECT 1 FROM feedback GROUP BY project_id, writer_id, target_id HAVING COUNT(*) > 1) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate feedback: manual resolution required';
 END IF;
 IF EXISTS (SELECT 1 FROM feedback WHERE rating NOT BETWEEN 1 AND 5 OR writer_id = target_id) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid feedback rating or self-feedback';
 END IF;
 IF EXISTS (SELECT 1 FROM project WHERE recruit_limit < 1 OR view_count < 0) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid project capacity or view count';
 END IF;
 IF EXISTS (SELECT 1 FROM user_interest_stack WHERE interest_stack IS NULL)
 OR EXISTS (SELECT 1 FROM user_interest_stack GROUP BY user_id, interest_stack HAVING COUNT(*) > 1)
 OR EXISTS (SELECT 1 FROM project_required_major WHERE required_major IS NULL)
 OR EXISTS (SELECT 1 FROM project_required_major GROUP BY project_id, required_major HAVING COUNT(*) > 1)
 OR EXISTS (SELECT 1 FROM project_required_stack WHERE required_stack IS NULL)
 OR EXISTS (SELECT 1 FROM project_required_stack GROUP BY project_id, required_stack HAVING COUNT(*) > 1) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Null or duplicate collection entries: manual resolution required';
 END IF;
 IF EXISTS (
  SELECT 1 FROM chat_message m JOIN chat_room r ON r.chat_room_id=m.chat_room_id
  JOIN project p ON p.project_id=r.project_id
  WHERE m.sender_id<>p.pm_id AND NOT EXISTS (
   SELECT 1 FROM project_application a WHERE a.project_id=p.project_id
   AND a.applicant_id=m.sender_id AND a.application_status='ACCEPTED')
 ) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Historical chat sender is not a project member';
 END IF;
 IF EXISTS (
  SELECT 1 FROM feedback f JOIN project p ON p.project_id=f.project_id
  WHERE (f.writer_id<>p.pm_id AND NOT EXISTS (
   SELECT 1 FROM project_application a WHERE a.project_id=p.project_id
   AND a.applicant_id=f.writer_id AND a.application_status='ACCEPTED'))
  OR (f.target_id<>p.pm_id AND NOT EXISTS (
   SELECT 1 FROM project_application a WHERE a.project_id=p.project_id
   AND a.applicant_id=f.target_id AND a.application_status='ACCEPTED'))
 ) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Historical feedback references a non-member';
 END IF;
END //
DELIMITER ;
CALL fat_assert_legacy_data();
DROP PROCEDURE fat_assert_legacy_data;

-- RENAME preserves identifiers, rows, legacy GitHub values and existing foreign keys.
RENAME TABLE `user` TO users, project TO projects,
 project_application TO applications, chat_room TO chat_rooms, chat_message TO chat_messages;

ALTER TABLE applications ADD CONSTRAINT uk_application_project_applicant UNIQUE (project_id, applicant_id);
ALTER TABLE user_interest_stack MODIFY interest_stack VARCHAR(255) NOT NULL,
 ADD PRIMARY KEY (user_id, interest_stack);
ALTER TABLE project_required_major MODIFY required_major VARCHAR(255) NOT NULL,
 ADD PRIMARY KEY (project_id, required_major);
ALTER TABLE project_required_stack MODIFY required_stack VARCHAR(255) NOT NULL,
 ADD PRIMARY KEY (project_id, required_stack);
ALTER TABLE projects ADD CONSTRAINT ck_project_capacity CHECK (recruit_limit > 0),
 ADD CONSTRAINT ck_project_views CHECK (view_count >= 0);

CREATE TABLE project_members (
 project_member_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 project_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
 CONSTRAINT uk_project_member UNIQUE (project_id, user_id),
 CONSTRAINT fk_member_project FOREIGN KEY (project_id) REFERENCES projects(project_id),
 CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;
INSERT INTO project_members (project_id, user_id)
 SELECT project_id, pm_id FROM projects
 UNION
 SELECT project_id, applicant_id FROM applications WHERE application_status='ACCEPTED';

CREATE TABLE chat_participants (
 chat_participant_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 chat_room_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
 CONSTRAINT uk_chat_participant UNIQUE (chat_room_id, user_id),
 CONSTRAINT fk_participant_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(chat_room_id),
 CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;
INSERT INTO chat_participants (chat_room_id, user_id)
 SELECT r.chat_room_id, m.user_id FROM chat_rooms r
 JOIN project_members m ON m.project_id=r.project_id;
ALTER TABLE chat_messages ADD CONSTRAINT fk_message_participant
 FOREIGN KEY (chat_room_id, sender_id) REFERENCES chat_participants(chat_room_id, user_id);
CREATE INDEX ix_messages_room_time ON chat_messages(chat_room_id, sent_at, chat_message_id);

ALTER TABLE feedback ADD CONSTRAINT uk_feedback_project_writer_target UNIQUE (project_id, writer_id, target_id),
 ADD CONSTRAINT ck_feedback_rating CHECK (rating BETWEEN 1 AND 5),
 ADD CONSTRAINT ck_feedback_not_self CHECK (writer_id <> target_id),
 ADD CONSTRAINT fk_feedback_writer_member FOREIGN KEY (project_id, writer_id) REFERENCES project_members(project_id, user_id),
 ADD CONSTRAINT fk_feedback_target_member FOREIGN KEY (project_id, target_id) REFERENCES project_members(project_id, user_id);

-- The base branch's InquiryMessage was incomplete and could not create a table.
-- Retain any pre-existing Inquiry_message/inquiry_message tables for manual review.
CREATE TABLE inquiry_messages (
 inquiry_message_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 inquiry_id BIGINT NOT NULL, sender_id BIGINT NOT NULL,
 message_content TEXT NOT NULL, sent_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_inquiry_message_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiry(inquiry_id),
 CONSTRAINT fk_inquiry_message_sender FOREIGN KEY (sender_id) REFERENCES users(user_id),
 INDEX ix_inquiry_messages_time (inquiry_id, sent_at, inquiry_message_id)
) ENGINE=InnoDB;
