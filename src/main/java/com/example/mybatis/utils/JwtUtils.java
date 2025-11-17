package com.example.mybatis.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

/**
 * Jwt 工具类：统一生成与解析 Token
 */
@Component
public class JwtUtils {

    private final Key signingKey;
    private final long expireMs;

    // 从 application.yml 中读取配置
    public JwtUtils(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expire-ms:3600000}") long expireMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMs = expireMs;
    }

    /** 生成 Token */
    public String generateToken(Long userId, String username, List<String> roles) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析 Claims */
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** 从 Token 提取 userId */
    public Long parseUserId(String token) {
        Claims c = parseClaims(token);
        Object v = c.get("userId");
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) return Long.valueOf((String) v);
        return null;
    }
}
