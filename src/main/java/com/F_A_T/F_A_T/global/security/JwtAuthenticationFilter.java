package com.F_A_T.F_A_T.global.security;

import com.F_A_T.F_A_T.domain.auth.repository.RefreshTokenRepository;
import com.F_A_T.F_A_T.domain.user.repository.UserRepository;
import com.F_A_T.F_A_T.global.exception.ErrorCode;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final TokenService tokens;
    private final RefreshTokenRepository sessions;
    private final UserRepository users;
    private final SecurityErrors errors;
    public JwtAuthenticationFilter(TokenService tokens, RefreshTokenRepository sessions, UserRepository users, SecurityErrors errors) {
        this.tokens=tokens;this.sessions=sessions;this.users=users;this.errors=errors;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header=request.getHeader("Authorization");
        if (header != null) {
            try {
                if (!header.startsWith("Bearer ") || header.length()<=7) throw new IllegalStateException();
                var jwt=tokens.decode(header.substring(7));
                Long userId=Long.valueOf(jwt.getSubject());
                Long sessionId=((Number)jwt.getClaim("sid")).longValue();
                var session=sessions.findById(sessionId).orElseThrow();
                if (!session.getUser().getUser_id().equals(userId) || session.getRevokedAt()!=null
                        || !session.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) throw new IllegalStateException();
                var principal=new CustomUserDetails(users.findById(userId).orElseThrow(),sessionId);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
            } catch (org.springframework.dao.DataAccessException ex) {
                errors.write(response,ErrorCode.INTERNAL_SERVER_ERROR);return;
            } catch (RuntimeException ex) {
                SecurityContextHolder.clearContext();errors.write(response,ErrorCode.UNAUTHORIZED);return;
            }
        }
        chain.doFilter(request,response);
    }
}
