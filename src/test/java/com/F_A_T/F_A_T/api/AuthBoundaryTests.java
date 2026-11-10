package com.F_A_T.F_A_T.api;

import com.F_A_T.F_A_T.domain.auth.service.*;
import com.F_A_T.F_A_T.global.exception.*;
import com.F_A_T.F_A_T.global.security.TokenService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.oauth2.jwt.JwtException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthBoundaryTests {
    final String key="boundary-tests-only-signing-key-at-least-32-bytes";
    String signed(String issuer,Instant expiry,String signingKey) throws Exception {
        var token=new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),new JWTClaimsSet.Builder()
                .issuer(issuer).subject("7").issueTime(Date.from(Instant.now().minusSeconds(3600)))
                .expirationTime(Date.from(expiry)).claim("sid",10).build());
        token.sign(new MACSigner(signingKey));return token.serialize();
    }
    @Test void jwtClaimsAndLifetimeAreVerified() {
        var tokens=new TokenService(key,"fat-tests",3600);
        var decoded=tokens.decode(tokens.issue(7L,10L));
        assertEquals("7",decoded.getSubject());assertEquals(10L,((Number)decoded.getClaim("sid")).longValue());
        assertEquals(3600,java.time.Duration.between(decoded.getIssuedAt(),decoded.getExpiresAt()).getSeconds());
    }
    @Test void wrongSigningKeyIsRejected() throws Exception {
        var tokens=new TokenService(key,"fat-tests",3600);
        assertThrows(JwtException.class,() -> tokens.decode(signed("fat-tests",Instant.now().plusSeconds(3600),"different-test-signing-key-more-than-32-bytes")));
    }
    @Test void wrongIssuerIsRejected() throws Exception {
        var tokens=new TokenService(key,"fat-tests",3600);
        assertThrows(JwtException.class,() -> tokens.decode(signed("other-service",Instant.now().plusSeconds(3600),key)));
    }
    @Test void expiredJwtIsRejected() throws Exception {
        var tokens=new TokenService(key,"fat-tests",3600);
        assertThrows(JwtException.class,() -> tokens.decode(signed("fat-tests",Instant.now().minusSeconds(120),key)));
    }
    @Test void insecureDefaultKeysAreRefused() {
        assertThrows(IllegalStateException.class,() -> new TokenService("short","fat-tests",3600));
        assertThrows(IllegalStateException.class,() -> new AuthCrypto("short"));
    }
    @SuppressWarnings("unchecked")
    @Test void smtpAdapterBuildsMailWithoutReturningCodeToCaller() {
        ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);var transport=mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(transport);
        new SmtpEmailSender(provider,"noreply@test.invalid").sendCode("student@dsm.hs.kr","123456");
        var sent=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);verify(transport).send(sent.capture());
        assertArrayEquals(new String[]{"student@dsm.hs.kr"},sent.getValue().getTo());
        assertTrue(sent.getValue().getText().contains("123456"));
    }
    @SuppressWarnings("unchecked")
    @Test void smtpFailureBecomesSafeServiceUnavailableError() {
        ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);var transport=mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(transport);
        doThrow(new MailSendException("sensitive-provider-diagnostic")).when(transport).send(any(SimpleMailMessage.class));
        var ex=assertThrows(ApiException.class,() -> new SmtpEmailSender(provider,"noreply@test.invalid").sendCode("student@dsm.hs.kr","123456"));
        assertEquals(ErrorCode.MAIL_DELIVERY_FAILED,ex.getErrorCode());assertFalse(ex.getMessage().contains("sensitive"));
    }
}
