package com.F_A_T.F_A_T.database;

import java.sql.*;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class MigrationTests {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withUsername("root").withPassword("disposable-test-only");
    String url;

    @BeforeEach
    void newIsolatedSchema() throws Exception {
        String name = "migration_" + UUID.randomUUID().toString().replace("-", "");
        try (var c = connection(MYSQL.getJdbcUrl()); var s = c.createStatement()) {
            s.execute("CREATE DATABASE " + name);
        }
        url = MYSQL.getJdbcUrl().replace("/" + MYSQL.getDatabaseName(), "/" + name);
        // Do not clean or reset any existing database. Each test owns a new schema in its container.
    }

    Connection connection(String jdbc) throws SQLException {
        return DriverManager.getConnection(jdbc, MYSQL.getUsername(), MYSQL.getPassword());
    }

    Flyway flyway(String target) {
        var config = Flyway.configure().dataSource(url, MYSQL.getUsername(), MYSQL.getPassword())
                .cleanDisabled(true).baselineOnMigrate(false);
        if (target != null) config.target(target);
        return config.load();
    }

    void sql(String statement) throws Exception {
        try (var c = connection(url); var s = c.createStatement()) { s.execute(statement); }
    }

    long count(String query) throws Exception {
        try (var c = connection(url); var s = c.createStatement(); var r = s.executeQuery(query)) {
            assertTrue(r.next()); return r.getLong(1);
        }
    }

    void legacyFixture() throws Exception {
        flyway("1").migrate();
        sql("INSERT INTO `user` (user_id,user_password,user_email,user_gender,user_major,user_student_number,user_name,github_username) VALUES " +
                "(10,'hash','pm@test.invalid','X','CS',20260001,'PM','legacy-name')," +
                "(20,'hash','member@test.invalid','X','CS',20260002,'Member',NULL)," +
                "(30,'hash','outsider@test.invalid','X','CS',20260003,'Outsider',NULL)");
        sql("INSERT INTO project VALUES (100,10,'title','description','2027-01-01','COMPLETED',7,4)");
        sql("INSERT INTO project_application VALUES (200,100,20,'hello','ACCEPTED')");
        sql("INSERT INTO chat_room VALUES (300,100)");
        sql("INSERT INTO chat_message VALUES (400,300,20,'preserve me','2026-01-01 12:00:00')");
        sql("INSERT INTO feedback VALUES (500,100,10,20,5,'thanks')");
        sql("INSERT INTO user_interest_stack VALUES (20,'Java')");
        sql("INSERT INTO project_required_major VALUES (100,'CS')");
        sql("INSERT INTO project_required_stack VALUES (100,'Java')");
        sql("INSERT INTO inquiry VALUES (600,100,30)");
    }

    @Test void emptySchemaMigratesAndIsRepeatable() throws Exception {
        assertEquals(3, flyway(null).migrate().migrationsExecuted);
        assertEquals(0, flyway(null).migrate().migrationsExecuted);
        assertEquals(0, count("SELECT COUNT(*) FROM users"));
    }

    @Test void legacyDataAndPrimaryKeysSurvive() throws Exception {
        legacyFixture();
        sql("ALTER TABLE chat_room RENAME INDEX uk_room_project TO UK_hibernate_legacy_project");
        flyway(null).migrate();
        assertEquals(1, count("SELECT COUNT(*) FROM users WHERE user_id=10 AND github_username='legacy-name'"));
        assertEquals(1, count("SELECT COUNT(*) FROM projects WHERE project_id=100 AND view_count=7"));
        assertEquals(1, count("SELECT COUNT(*) FROM applications WHERE application_id=200 AND application_status='ACCEPTED'"));
        assertEquals(1, count("SELECT COUNT(*) FROM chat_messages WHERE chat_message_id=400 AND message_content='preserve me'"));
        assertEquals(1, count("SELECT COUNT(*) FROM feedback WHERE feedback_id=500 AND feedback_content='thanks'"));
        assertEquals(1, count("SELECT COUNT(*) FROM inquiry WHERE inquiry_id=600"));
        assertEquals(1, count("SELECT COUNT(*) FROM user_interest_stack WHERE user_id=20 AND interest_stack='Java'"));
        assertEquals(1, count("SELECT COUNT(*) FROM project_required_major WHERE project_id=100 AND required_major='CS'"));
        assertEquals(1, count("SELECT COUNT(*) FROM project_required_stack WHERE project_id=100 AND required_stack='Java'"));
        assertEquals(2, count("SELECT COUNT(*) FROM project_members WHERE project_id=100"));
        assertEquals(2, count("SELECT COUNT(*) FROM chat_participants WHERE chat_room_id=300"));
        sql("INSERT INTO chat_rooms (project_id,room_type,requester_id) VALUES (100,'DIRECT',30)");
        assertEquals(2, count("SELECT COUNT(*) FROM chat_rooms WHERE project_id=100"));
        assertThrows(SQLException.class, () -> sql("INSERT INTO chat_rooms (project_id,room_type,requester_id) VALUES (100,'DIRECT',30)"));
        assertEquals(0, flyway(null).migrate().migrationsExecuted);
    }

    @Test void duplicateApplicationsStopBeforeRenamingAndKeepRows() throws Exception {
        legacyFixture();
        sql("INSERT INTO project_application VALUES (201,100,20,'duplicate','PENDING')");
        assertThrows(Exception.class, () -> flyway(null).migrate());
        assertEquals(2, count("SELECT COUNT(*) FROM project_application"));
        assertEquals(3, count("SELECT COUNT(*) FROM `user`"));
        assertEquals(1, count("SELECT COUNT(*) FROM chat_message"));
    }

    @Test void invalidHistoricalChatStopsWithoutGrantingMembership() throws Exception {
        legacyFixture();
        sql("UPDATE chat_message SET sender_id=30 WHERE chat_message_id=400");
        assertThrows(Exception.class, () -> flyway(null).migrate());
        assertEquals(1, count("SELECT COUNT(*) FROM chat_message WHERE sender_id=30"));
        assertEquals(1, count("SELECT COUNT(*) FROM project_application"));
    }

    @Test void duplicateAndNullCollectionsAreNotSilentlyDiscarded() throws Exception {
        legacyFixture();
        sql("INSERT INTO user_interest_stack VALUES (20,'Java'),(20,NULL)");
        assertThrows(Exception.class, () -> flyway(null).migrate());
        assertEquals(3, count("SELECT COUNT(*) FROM user_interest_stack"));
    }

    @Test void invalidFeedbackStopsBeforeRenaming() throws Exception {
        legacyFixture();
        sql("UPDATE feedback SET rating=6");
        assertThrows(Exception.class, () -> flyway(null).migrate());
        assertEquals(1, count("SELECT COUNT(*) FROM feedback WHERE rating=6"));
        assertEquals(3, count("SELECT COUNT(*) FROM `user`"));
    }

    @Test void existingUnversionedSchemaNeedsExplicitBaseline() throws Exception {
        try (var c = connection(url)) {
            ScriptUtils.executeSqlScript(c, new ClassPathResource("db/migration/V1__legacy_schema.sql"));
        }
        assertThrows(Exception.class, () -> flyway(null).migrate());
        var baseline = Flyway.configure().dataSource(url, MYSQL.getUsername(), MYSQL.getPassword())
                .baselineVersion("1").cleanDisabled(true).load();
        baseline.baseline();
        assertEquals(2, baseline.migrate().migrationsExecuted);
    }

    @Test void mysqlEnforcesMembershipUniquenessAndChecks() throws Exception {
        legacyFixture(); flyway(null).migrate();
        assertThrows(SQLException.class, () -> sql("INSERT INTO applications (project_id,applicant_id,application_status) VALUES (100,20,'PENDING')"));
        assertThrows(SQLException.class, () -> sql("INSERT INTO project_members (project_id,user_id) VALUES (100,20)"));
        assertThrows(SQLException.class, () -> sql("INSERT INTO project_members (project_id,user_id) VALUES (100,999)"));
        assertThrows(SQLException.class, () -> sql("INSERT INTO chat_participants (chat_room_id,user_id) VALUES (300,20)"));
        assertThrows(SQLException.class, () -> sql("INSERT INTO chat_messages (chat_room_id,sender_id,message_content,sent_at) VALUES (300,30,'not participant',NOW())"));
        assertThrows(SQLException.class, () -> sql("INSERT INTO feedback (project_id,writer_id,target_id,rating) VALUES (100,20,10,0)"));
        assertThrows(SQLException.class, () -> sql("INSERT INTO feedback (project_id,writer_id,target_id,rating) VALUES (100,20,20,5)"));
        assertThrows(SQLException.class, () -> sql("INSERT INTO feedback (project_id,writer_id,target_id,rating) VALUES (100,10,30,5)"));
        assertThrows(SQLException.class, () -> sql("DELETE FROM users WHERE user_id=20"));
        assertThrows(SQLException.class, () -> sql("INSERT INTO users (user_password,user_email,user_gender,user_major,user_student_number,user_name) VALUES ('hash','pm@test.invalid','X','CS',1,'dup')"));
        assertThrows(SQLException.class, () -> sql("UPDATE users SET user_name=NULL WHERE user_id=20"));
        assertThrows(SQLException.class, () -> sql("INSERT INTO feedback (project_id,writer_id,target_id,rating) VALUES (100,10,20,5)"));
        assertThrows(SQLException.class, () -> sql("INSERT INTO chat_rooms (project_id) VALUES (100)"));
        assertThrows(SQLException.class, () -> sql("INSERT INTO user_interest_stack VALUES (20,'Java')"));
        assertThrows(SQLException.class, () -> sql("INSERT INTO chat_rooms (project_id,room_type,requester_id) VALUES (100,'TEAM',30)"));
        assertThrows(SQLException.class, () -> sql("INSERT INTO chat_rooms (project_id,room_type) VALUES (100,'DIRECT')"));
        sql("INSERT INTO chat_rooms (chat_room_id,project_id,room_type,requester_id) VALUES (301,100,'DIRECT',30)");
        sql("INSERT INTO chat_participants (chat_room_id,user_id) VALUES (301,30)");
        sql("INSERT INTO chat_messages VALUES (401,301,30,'direct',NOW())");
        assertThrows(SQLException.class, () -> sql("UPDATE chat_participants SET last_read_message_id=401 WHERE chat_room_id=300 AND user_id=20"));
    }
}
