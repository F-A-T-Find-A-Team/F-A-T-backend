package com.F_A_T.F_A_T.global.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.*;

@Component
public class TokenService {
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final String issuer;
    private final long accessSeconds;
    public TokenService(@Value("${app.jwt.secret}") String secret,
                        @Value("${app.jwt.issuer:fat-backend}") String issuer,
                        @Value("${app.jwt.access-seconds:3600}") long accessSeconds) {
        if (secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        var key=new SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256");
        this.encoder=new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var configured= NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        configured.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        this.decoder=configured;
        this.issuer=issuer;
        this.accessSeconds=accessSeconds;
    }
    public String issue(Long userId, Long sessionId) {
        var now=Instant.now();
        var claims=JwtClaimsSet.builder().issuer(issuer).subject(userId.toString())
                .issuedAt(now).expiresAt(now.plusSeconds(accessSeconds)).id(UUID.randomUUID().toString())
                .claim("sid",sessionId).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
    }
    public Jwt decode(String token) { return decoder.decode(token); }
    public long accessSeconds() { return accessSeconds; }
}
