package com.example.mybatis.utils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 * JWT 工具类：双 Token + Redis
 * @author yihui
 */
@Component
@RequiredArgsConstructor
public class JwtUtils {
    private final RedisUtil redisUtil;
    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.access-token-expire-ms:1800000}") // 30分钟
    private long accessTokenExpireMs;
    @Value("${jwt.refresh-token-expire-ms:604800000}") // 7天
    private long refreshTokenExpireMs;
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    // ========== Access Token ==========
    /**
     * 生成 Access Token
     */
    public String generateAccessToken(Long userId, String username, List<String> roles) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpireMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    // ========== Refresh Token ==========
    /**
     * 生成 Refresh Token 并存入 Redis
     */
    public String generateRefreshToken(Long userId, String username) {
        String refreshToken = Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpireMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
        // 存入 Redis（key: refresh:userId, value: refreshToken）
        String redisKey = "refresh:" + userId;
        redisUtil.set(redisKey, refreshToken, refreshTokenExpireMs, TimeUnit.MILLISECONDS);
        return refreshToken;
    }
    /**
     * 验证 Refresh Token 是否有效
     */
    public boolean validateRefreshToken(String refreshToken) {
        try {
            Claims claims = parseClaims(refreshToken);
            Long userId = claims.get("userId", Long.class);
            String type = claims.get("type", String.class);
            // 检查类型
            if (!"refresh".equals(type)) {
                return false;
            }
            // 检查 Redis 中是否存在且匹配
            String redisKey = "refresh:" + userId;
            String storedToken = redisUtil.get(redisKey);
            return refreshToken.equals(storedToken);
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * 删除 Refresh Token（登出时调用）
     */
    public void deleteRefreshToken(Long userId) {
        redisUtil.delete("refresh:" + userId);
    }
    // ========== 通用方法 ==========
    /**
     * 解析 Claims
     */
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}