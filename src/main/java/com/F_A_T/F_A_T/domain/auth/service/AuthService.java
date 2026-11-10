package com.F_A_T.F_A_T.domain.auth.service;

import com.F_A_T.F_A_T.domain.auth.dto.AuthRequests;
import com.F_A_T.F_A_T.domain.auth.entity.*;
import com.F_A_T.F_A_T.domain.auth.repository.*;
import com.F_A_T.F_A_T.domain.user.entity.User;
import com.F_A_T.F_A_T.domain.user.repository.UserRepository;
import com.F_A_T.F_A_T.global.exception.*;
import com.F_A_T.F_A_T.global.security.TokenService;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final EmailVerificationRepository verifications;
    private final RefreshTokenRepository sessions;
    private final AuthCrypto crypto;
    private final EmailSender emails;
    private final PasswordEncoder passwords;
    private final TokenService jwt;
    private final JdbcTemplate jdbc;
    public record SessionResponse(Map<String,Object> data,String refreshToken,long refreshSeconds) {}
    public AuthService(UserRepository users,EmailVerificationRepository verifications,RefreshTokenRepository sessions,
                       AuthCrypto crypto,EmailSender emails,PasswordEncoder passwords,TokenService jwt,JdbcTemplate jdbc) {
        this.users=users;this.verifications=verifications;this.sessions=sessions;this.crypto=crypto;
        this.emails=emails;this.passwords=passwords;this.jwt=jwt;this.jdbc=jdbc;
    }
    private LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
    private String email(String input) { return input.trim().toLowerCase(Locale.ROOT); }
    @Transactional
    public Map<String,Object> send(AuthRequests.Email request) {
        String email=email(request.email());
        if (!email.endsWith("@dsm.hs.kr") || email.indexOf('@')!=email.lastIndexOf('@')) throw new ApiException(ErrorCode.INVALID_EMAIL_DOMAIN);
        if (request.purpose().equals("VERIFY_ACCOUNT")) {
            var existing=users.findByUserEmail(email).orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
            if (request.password()==null || request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72
                    || !passwords.matches(request.password(),existing.getUser_password())) throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        } else if (users.existsByUserEmail(email)) throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        jdbc.update("INSERT INTO email_delivery_limits(email,last_sent_at) VALUES (?,?) ON DUPLICATE KEY UPDATE email=email",email,LocalDateTime.of(1970,1,1,0,0));
        LocalDateTime previous=jdbc.queryForObject("SELECT last_sent_at FROM email_delivery_limits WHERE email=? FOR UPDATE",LocalDateTime.class,email);
        LocalDateTime now=now();
        if (previous.plusSeconds(60).isAfter(now)) throw new ApiException(ErrorCode.TOO_MANY_REQUESTS);
        String code=crypto.code();
        var verification=verifications.saveAndFlush(new EmailVerification(email,crypto.codeHash(email,code),now.plusMinutes(5),request.purpose()));
        emails.sendCode(email,code);
        jdbc.update("UPDATE email_delivery_limits SET last_sent_at=? WHERE email=?",now,email);
        return Map.of("verificationId","ev_"+verification.getId(),"expiresInSeconds",300,"resendAfterSeconds",60);
    }
    @Transactional(noRollbackFor=VerificationFailureException.class)
    public Map<String,Object> verify(AuthRequests.Verify request) {
        Long id;
        try { id=Long.valueOf(request.verificationId().replaceFirst("^ev_", "")); }
        catch (NumberFormatException ex) { throw new ApiException(ErrorCode.INVALID_VERIFICATION_CODE); }
        var verification=verifications.findForUpdate(id).orElseThrow(() -> new ApiException(ErrorCode.INVALID_VERIFICATION_CODE));
        LocalDateTime now=now();
        if (verification.getAttempts()>=5) throw new ApiException(ErrorCode.TOO_MANY_ATTEMPTS);
        if (!verification.getCodeExpiresAt().isAfter(now)) throw new ApiException(ErrorCode.VERIFICATION_EXPIRED);
        if (verification.getVerifiedAt()!=null) throw new ApiException(ErrorCode.INVALID_VERIFICATION_CODE);
        var latest=verifications.findFirstByEmailOrderByIdDesc(verification.getEmail());
        if (latest.isEmpty() || !latest.get().getId().equals(id)) throw new ApiException(ErrorCode.INVALID_VERIFICATION_CODE);
        if (!crypto.matches(crypto.codeHash(verification.getEmail(),request.code()),verification.getCodeHash())) {
            verification.recordFailedAttempt();
            throw new VerificationFailureException(verification.getAttempts()>=5 ? ErrorCode.TOO_MANY_ATTEMPTS : ErrorCode.INVALID_VERIFICATION_CODE);
        }
        if (verification.getPurpose().equals("VERIFY_ACCOUNT")) {
            var existing=users.findByUserEmail(verification.getEmail()).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
            existing.setEmailVerified(true);verification.markVerified(null,null,now);
            return Map.of("verified",true);
        }
        String token=crypto.token();verification.markVerified(crypto.hash(token),now.plusMinutes(10),now);
        return Map.of("verified",true,"signupToken",token);
    }
    @Transactional
    public Map<String,Object> signup(AuthRequests.Signup request) {
        String email=email(request.email());
        if (!email.endsWith("@dsm.hs.kr") || request.avatarFileId()!=null
                || request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new ApiException(ErrorCode.INVALID_SIGNUP_DATA);
        if (users.existsByUserEmail(email)) throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        if (users.nicknameExists(request.nickname(),null)) throw new ApiException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        var verification=verifications.findBySignupTokenHash(crypto.hash(request.signupToken())).orElseThrow(() -> new ApiException(ErrorCode.INVALID_SIGNUP_TOKEN));
        if (!verification.getEmail().equals(email) || verifications.consumeSignupToken(crypto.hash(request.signupToken()),now())!=1) throw new ApiException(ErrorCode.INVALID_SIGNUP_TOKEN);
        if (new HashSet<>(request.majors()).size()!=request.majors().size() || new HashSet<>(request.techStacks()).size()!=request.techStacks().size()) throw new ApiException(ErrorCode.INVALID_SIGNUP_DATA);
        var user=User.builder().user_email(email).user_password(passwords.encode(request.password()))
                .user_name(request.nickname()).user_gender(request.gender()).user_major(request.majors().get(0))
                .user_student_number(Integer.valueOf(request.studentId())).interest_stacks(new ArrayList<>(request.techStacks())).build();
        user.setNickname(request.nickname());user.setRealName(request.name());user.setStudentId(request.studentId());
        user.setMajors(new ArrayList<>(request.majors()));user.setUser_bio(request.bio());user.setEmailVerified(true);
        users.saveAndFlush(user);
        return Map.of("userId",user.getUser_id(),"email",email,"nickname",user.getNickname(),"emailVerified",true);
    }
    @Transactional
    public SessionResponse login(AuthRequests.Login request) {
        var user=users.findByUserEmail(email(request.email())).orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
        if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72 || !passwords.matches(request.password(),user.getUser_password())) throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        if (!user.isEmailVerified()) throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED);
        String refresh=crypto.token();LocalDateTime now=now();
        var session=sessions.saveAndFlush(new RefreshToken(user,crypto.hash(refresh),now,now.plusDays(14)));
        var data=new LinkedHashMap<String,Object>(tokenData(user.getUser_id(),session.getId()));
        data.put("user",Map.of("id",user.getUser_id(),"nickname",user.getNickname(),"majors",List.copyOf(user.getMajors())));
        return new SessionResponse(data,refresh,14*86400);
    }
    @Transactional
    public SessionResponse refresh(String rawToken) {
        if (rawToken==null || rawToken.isBlank()) throw new ApiException(ErrorCode.REFRESH_TOKEN_MISSING);
        var session=sessions.findForUpdateByTokenHash(crypto.hash(rawToken)).orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_REVOKED));
        if (session.getRevokedAt()!=null) throw new ApiException(ErrorCode.REFRESH_TOKEN_REVOKED);
        if (!session.getExpiresAt().isAfter(now())) throw new ApiException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        String next=crypto.token();session.rotate(crypto.hash(next));
        long remaining=Math.max(1,Duration.between(now(),session.getExpiresAt()).getSeconds());
        return new SessionResponse(tokenData(session.getUser().getUser_id(),session.getId()),next,remaining);
    }
    @Transactional
    public void logout(Long userId,Long sessionId) {
        var session=sessions.findSessionForUpdate(sessionId).orElseThrow(() -> new ApiException(ErrorCode.INVALID_SESSION));
        if (!session.getUser().getUser_id().equals(userId) || session.getRevokedAt()!=null) throw new ApiException(ErrorCode.INVALID_SESSION);
        session.revoke(now());
    }
    private Map<String,Object> tokenData(Long userId,Long sessionId) {
        return Map.of("accessToken",jwt.issue(userId,sessionId),"tokenType","Bearer","expiresInSeconds",jwt.accessSeconds());
    }
}
