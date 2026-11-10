package com.F_A_T.F_A_T.api;

import com.F_A_T.F_A_T.domain.auth.service.EmailSender;
import java.net.*;
import java.net.http.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import tools.jackson.databind.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"app.auth.secure-cookie=true","app.avatar.storage-path=/tmp/fat-api-flow-avatars"})
@Testcontainers
@Import(ApiFlowTests.MailConfig.class)
class ApiFlowTests {
    @Container static final MySQLContainer<?> MYSQL=new MySQLContainer<>("mysql:8.4");
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",MYSQL::getJdbcUrl);r.add("spring.datasource.username",MYSQL::getUsername);r.add("spring.datasource.password",MYSQL::getPassword);
    }
    @TestConfiguration static class MailConfig {
        @Bean @Primary Mailbox mailbox() { return new Mailbox(); }
    }
    static class Mailbox implements EmailSender {
        final Map<String,String> codes=new ConcurrentHashMap<>();
        public void sendCode(String email,String code) { codes.put(email,code); }
    }
    @Value("${local.server.port}") int port;
    @Autowired ObjectMapper mapper;
    @Autowired Mailbox mail;
    @Autowired JdbcTemplate jdbc;
    final HttpClient client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    record Result(int status,JsonNode json,HttpHeaders headers) {}
    record Account(long id,String email,String token,String cookie) {}
    String unique() { return UUID.randomUUID().toString().replace("-",""); }
    Result request(String method,String path,Object body,String token,String cookie) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).header("Content-Type","application/json");
        if(token!=null)b.header("Authorization","Bearer "+token);
        if(cookie!=null)b.header("Cookie",cookie);
        b.method(method,body==null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        var r=client.send(b.build(),HttpResponse.BodyHandlers.ofString());
        return new Result(r.statusCode(),r.body().isBlank()?null:mapper.readTree(r.body()),r.headers());
    }
    JsonNode success(Result r,int code) {
        assertEquals(code,r.status(),()->String.valueOf(r.json()));
        assertTrue(r.json().path("success").asBoolean());return r.json().path("data");
    }
    void error(Result r,int status,String code) {
        assertEquals(status,r.status(),()->String.valueOf(r.json()));assertFalse(r.json().path("success").asBoolean());
        assertEquals(code,r.json().path("error").path("code").asText());
        assertFalse(r.json().has("trace"));assertFalse(r.json().has("exception"));
    }
    Map<String,Object> signup(String token,String email,String nickname) {
        return Map.of("signupToken",token,"email",email,"password","Safe-test-password!","nickname",nickname,
                "studentId","1107","name","Test User","gender","MALE","bio","testing","majors",List.of("BE"),"techStacks",List.of("Java"));
    }
    Account account() throws Exception {
        String label=unique(),email=label+"@dsm.hs.kr";
        var sent=success(request("POST","/auth/email/send",Map.of("email",email,"purpose","SIGNUP"),null,null),200);
        assertFalse(sent.has("code"));String vid=sent.path("verificationId").asText();
        var verified=success(request("POST","/auth/email/verify",Map.of("verificationId",vid,"code",mail.codes.get(email)),null,null),200);
        String signupToken=verified.path("signupToken").asText();
        var user=success(request("POST","/auth/signup",signup(signupToken,email,label),null,null),201);
        error(request("POST","/auth/signup",signup(signupToken,email,label),null,null),409,"EMAIL_ALREADY_EXISTS");
        var login=request("POST","/auth/login",Map.of("email",email,"password","Safe-test-password!"),null,null);
        var data=success(login,200);
        String cookie=login.headers().firstValue("set-cookie").orElseThrow();
        assertTrue(cookie.contains("HttpOnly"));assertTrue(cookie.contains("Secure"));assertTrue(cookie.contains("SameSite=Strict"));
        return new Account(user.path("userId").asLong(),email,data.path("accessToken").asText(),cookie.split(";",2)[0]);
    }
    @Test void completeAuthenticatedApiJourney() throws Exception {
        var pm=account();var member=account();var outsider=account();
        var me=success(request("GET","/users/me",null,pm.token(),null),200);
        assertEquals(pm.email(),me.path("email").asText());
        String nick="changed"+unique();
        success(request("PATCH","/users/me",Map.of("nickname",nick,"gender","FEMALE","bio","updated","majors",List.of("BE","AI"),"techStacks",List.of("Java","Python")),pm.token(),null),200);
        assertEquals(nick,success(request("GET","/users/me",null,pm.token(),null),200).path("nickname").asText());
        var pub=success(request("GET","/users/"+pm.id(),null,member.token(),null),200);
        assertFalse(pub.has("email"));assertFalse(pub.has("studentId"));assertFalse(pub.has("name"));
        // Real multipart upload, generated storage URL and subsequent safe retrieval.
        var image=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",image);
        String boundary="fat"+unique();var multipart=new ByteArrayOutputStream();
        multipart.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"test.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes());
        multipart.write(image.toByteArray());multipart.write(("\r\n--"+boundary+"--\r\n").getBytes());
        var upload=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/users/me/avatar"))
                .header("Authorization","Bearer "+pm.token()).header("Content-Type","multipart/form-data; boundary="+boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipart.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,upload.statusCode(),upload.body());
        String avatar=mapper.readTree(upload.body()).path("data").path("avatarUrl").asText();
        assertTrue(avatar.startsWith("/avatars/"));
        assertEquals(200,client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+avatar)).GET().build(),HttpResponse.BodyHandlers.discarding()).statusCode());
        var created=success(request("POST","/projects",Map.of("title","Contract test","description","API journey","requiredMajors",List.of("BE"),"techStacks",List.of("Java"),"deadline",LocalDate.now().plusDays(10).toString(),"capacity",3),pm.token(),null),201);
        long project=created.path("id").asLong();assertTrue(project>0);assertEquals(1,created.path("memberCount").asInt());
        success(request("GET","/projects?query=Contract&major=BE&techStack=Java&page=0&size=10",null,member.token(),null),200);
        assertEquals(0,success(request("GET","/projects?query=never-match-"+unique(),null,member.token(),null),200).path("totalElements").asInt());
        success(request("GET","/projects/recommended?size=5",null,member.token(),null),200);
        success(request("GET","/projects/"+project,null,member.token(),null),200);
        success(request("PATCH","/projects/"+project,Map.of("title","Edited title","capacity",3),pm.token(),null),200);
        success(request("GET","/projects/"+project+"/members",null,pm.token(),null),200);
        var direct=success(request("POST","/chat-rooms/direct",Map.of("projectId",project),member.token(),null),200);
        long directId=direct.path("roomId").asLong();assertTrue(directId>0);
        assertFalse(success(request("POST","/chat-rooms/direct",Map.of("projectId",project),member.token(),null),200).path("created").asBoolean());
        var applied=success(request("POST","/projects/"+project+"/applications",Map.of("position","BE","reason","I can help"),member.token(),null),201);
        long application=applied.path("id").asLong();assertTrue(application>0);
        error(request("POST","/projects/"+project+"/applications",Map.of("position","BE","reason","again"),member.token(),null),409,"ALREADY_APPLIED");
        success(request("GET","/projects/"+project+"/applications?status=PENDING",null,pm.token(),null),200);
        success(request("GET","/users/me/applications",null,member.token(),null),200);
        var accepted=success(request("PATCH","/applications/"+application,Map.of("status","ACCEPTED"),pm.token(),null),200);
        long room=accepted.path("teamRoomId").asLong();assertTrue(room>0);
        error(request("PATCH","/applications/"+application,Map.of("status","ACCEPTED"),pm.token(),null),409,"ALREADY_REVIEWED");
        long cancelled=success(request("POST","/projects/"+project+"/applications",Map.of("position","BE","reason","later cancel"),outsider.token(),null),201).path("id").asLong();
        var cancel=request("DELETE","/applications/"+cancelled,null,outsider.token(),null);assertEquals(204,cancel.status());assertNull(cancel.json());
        success(request("GET","/users/me/projects?stage=IDEA",null,member.token(),null),200);
        success(request("POST","/chat-rooms/"+room+"/messages",Map.of("type","TEXT","content","hello","clientMessageId",unique()),pm.token(),null),201);
        var rooms=success(request("GET","/chat-rooms",null,member.token(),null),200);
        assertTrue(rooms.path("items").size()>=2);
        error(request("GET","/chat-rooms/"+room+"/messages",null,outsider.token(),null),403,"NOT_ROOM_PARTICIPANT");
        var messages=success(request("GET","/chat-rooms/"+room+"/messages?size=10",null,member.token(),null),200);
        assertEquals(1,messages.path("items").size());
        success(request("PATCH","/projects/"+project+"/stage",Map.of("stage","IN_PROGRESS"),pm.token(),null),200);
        success(request("PATCH","/projects/"+project+"/stage",Map.of("stage","COMPLETED"),pm.token(),null),200);
        var review=Map.of("reviews",List.of(Map.of("revieweeId",member.id(),"rating",5,"comment","Great")));
        success(request("POST","/projects/"+project+"/feedback",review,pm.token(),null),201);
        error(request("POST","/projects/"+project+"/feedback",review,pm.token(),null),409,"ALREADY_REVIEWED");
        success(request("GET","/projects/"+project+"/feedback-status",null,pm.token(),null),200);
        success(request("GET","/users/me/feedback",null,member.token(),null),200);
        var refreshed=request("POST","/auth/refresh",null,null,member.cookie());
        String nextToken=success(refreshed,200).path("accessToken").asText();
        String nextCookie=refreshed.headers().firstValue("set-cookie").orElseThrow().split(";",2)[0];
        error(request("POST","/auth/refresh",null,null,member.cookie()),401,"REFRESH_TOKEN_REVOKED");
        var logout=request("POST","/auth/logout",null,nextToken,nextCookie);assertEquals(204,logout.status());assertNull(logout.json());
        error(request("GET","/users/me",null,nextToken,null),401,"UNAUTHORIZED");
        error(request("POST","/auth/refresh",null,null,nextCookie),401,"REFRESH_TOKEN_REVOKED");
    }
    @Test void authFailuresUseSafeTypedErrorsAndPersistAttemptLimits() throws Exception {
        error(request("GET","/users/me",null,null,null),401,"UNAUTHORIZED");
        error(request("GET","/users/me",null,"not-a-jwt",null),401,"UNAUTHORIZED");
        error(request("POST","/auth/login",Map.of("email","missing@dsm.hs.kr","password","wrong"),null,null),401,"INVALID_CREDENTIALS");
        error(request("POST","/auth/login",Map.of(),null,null),400,"INVALID_REQUEST");
        error(request("POST","/auth/refresh",null,null,null),401,"REFRESH_TOKEN_MISSING");
        error(request("POST","/auth/email/send",Map.of("email","test@example.com","purpose","SIGNUP"),null,null),400,"INVALID_EMAIL_DOMAIN");
        String email=unique()+"@dsm.hs.kr";
        var sent=success(request("POST","/auth/email/send",Map.of("email",email,"purpose","SIGNUP"),null,null),200);
        String id=sent.path("verificationId").asText();
        error(request("POST","/auth/email/send",Map.of("email",email,"purpose","SIGNUP"),null,null),429,"TOO_MANY_REQUESTS");
        String wrong=mail.codes.get(email).equals("000000")?"111111":"000000";
        for(int i=0;i<4;i++)error(request("POST","/auth/email/verify",Map.of("verificationId",id,"code",wrong),null,null),400,"INVALID_VERIFICATION_CODE");
        error(request("POST","/auth/email/verify",Map.of("verificationId",id,"code",wrong),null,null),429,"TOO_MANY_ATTEMPTS");
        error(request("POST","/auth/email/verify",Map.of("verificationId",id,"code",mail.codes.get(email)),null,null),429,"TOO_MANY_ATTEMPTS");
        assertEquals(5,jdbc.queryForObject("SELECT attempts FROM email_verifications WHERE verification_id=?",Integer.class,Long.valueOf(id.substring(3))));
        error(request("POST","/auth/signup",signup("invalid",unique()+"@dsm.hs.kr",unique()),null,null),401,"INVALID_SIGNUP_TOKEN");
    }
    @Test void expiredAndConsumedVerificationTokensAreRejected() throws Exception {
        String email=unique()+"@dsm.hs.kr";
        String id=success(request("POST","/auth/email/send",Map.of("email",email,"purpose","SIGNUP"),null,null),200).path("verificationId").asText();
        jdbc.update("UPDATE email_verifications SET code_expires_at=TIMESTAMPADD(MINUTE,-1,UTC_TIMESTAMP()) WHERE verification_id=?",Long.valueOf(id.substring(3)));
        error(request("POST","/auth/email/verify",Map.of("verificationId",id,"code",mail.codes.get(email)),null,null),410,"VERIFICATION_EXPIRED");
        var existing=account();
        error(request("POST","/auth/login",Map.of("email",existing.email(),"password","wrong-password"),null,null),401,"INVALID_CREDENTIALS");
        error(request("GET","/projects/999999999",null,existing.token(),null),404,"PROJECT_NOT_FOUND");
        error(request("GET","/users/999999999",null,existing.token(),null),404,"USER_NOT_FOUND");
    }
    @Test void validationNeverLeaksInternalExceptionsAndLegacyUsersCanVerifyOwnership() throws Exception {
        var tooLong=new HashMap<String,Object>(signup("invalid",unique()+"@dsm.hs.kr",unique()));
        tooLong.put("password","한".repeat(30));
        error(request("POST","/auth/signup",tooLong,null,null),400,"INVALID_SIGNUP_DATA");
        tooLong.put("password","Safe-test-password!");tooLong.put("nickname","n".repeat(41));
        error(request("POST","/auth/signup",tooLong,null,null),400,"INVALID_SIGNUP_DATA");
        error(request("POST","/auth/email/verify",Map.of("verificationId","nonnumeric","code","bad"),null,null),400,"INVALID_VERIFICATION_CODE");
        var account=account();
        jdbc.update("UPDATE users SET email_verified=false WHERE user_id=?",account.id());
        error(request("POST","/auth/login",Map.of("email",account.email(),"password","Safe-test-password!"),null,null),403,"EMAIL_NOT_VERIFIED");
        jdbc.update("UPDATE email_delivery_limits SET last_sent_at=TIMESTAMPADD(MINUTE,-2,UTC_TIMESTAMP()) WHERE email=?",account.email());
        var sent=success(request("POST","/auth/email/send",Map.of("email",account.email(),"purpose","VERIFY_ACCOUNT","password","Safe-test-password!"),null,null),200);
        var verified=success(request("POST","/auth/email/verify",Map.of("verificationId",sent.path("verificationId").asText(),"code",mail.codes.get(account.email())),null,null),200);
        assertTrue(verified.path("verified").asBoolean());assertFalse(verified.has("signupToken"));
        success(request("POST","/auth/login",Map.of("email",account.email(),"password","Safe-test-password!"),null,null),200);
    }

}
