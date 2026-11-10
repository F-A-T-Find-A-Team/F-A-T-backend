package com.F_A_T.F_A_T.global.config;

import com.F_A_T.F_A_T.global.security.*;
import com.F_A_T.F_A_T.global.exception.ErrorCode;
import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
    @Bean SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwt, SecurityErrors errors) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(c -> {}).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .logout(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                    .requestMatchers("/auth/email/send","/auth/email/verify","/auth/signup","/auth/login","/auth/refresh").permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET,"/avatars/*.png").permitAll()
                    .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint((r,s,x) -> errors.write(s,ErrorCode.UNAUTHORIZED))
                    .accessDeniedHandler((r,s,x) -> errors.write(s,ErrorCode.FORBIDDEN)))
                .addFilterBefore(jwt,UsernamePasswordAuthenticationFilter.class).build();
    }
    @Bean FilterRegistrationBean<JwtAuthenticationFilter> jwtRegistration(JwtAuthenticationFilter filter) {
        var bean=new FilterRegistrationBean<>(filter);bean.setEnabled(false);return bean;
    }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.origins:http://localhost:3000}") List<String> origins) {
        var c=new CorsConfiguration();c.setAllowedOrigins(origins);c.setAllowedMethods(List.of("GET","POST","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization","Content-Type"));c.setAllowCredentials(true);
        var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",c);return source;
    }
}
