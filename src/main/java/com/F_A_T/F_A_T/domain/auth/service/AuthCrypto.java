package com.F_A_T.F_A_T.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthCrypto {
    private final byte[] secret;
    private final SecureRandom random=new SecureRandom();
    public AuthCrypto(@Value("${app.auth.code-secret}") String secret) {
        this.secret=secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length<32) throw new IllegalStateException("EMAIL_CODE_SECRET must contain at least 32 UTF-8 bytes");
    }
    public String token() { byte[] bytes=new byte[32];random.nextBytes(bytes);return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    public String code() { return String.format("%06d",random.nextInt(1000000)); }
    public String codeHash(String email,String code) {
        try { var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal((email+":"+code).getBytes(StandardCharsets.UTF_8))); }
        catch (GeneralSecurityException ex) { throw new IllegalStateException("HMAC unavailable"); }
    }
    public String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (GeneralSecurityException ex) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
    public boolean matches(String a,String b) { return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8)); }
}
