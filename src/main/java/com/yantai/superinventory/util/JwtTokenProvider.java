package com.yantai.superinventory.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Token 工具类
 * 用于生成和验证 JWT Token
 */
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    /**
     * 生成 JWT Token
     * 
     * @param userId 用户ID
     * @param openid 微信OpenID
     * @param nickname 用户昵称
     * @param avatarUrl 用户头像URL
     * @param role 用户角色
     * @return JWT Token
     */
    public String generateToken(UUID userId, String openid, String nickname, String avatarUrl, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("openid", openid);
        claims.put("nickname", nickname);
        claims.put("avatarUrl", avatarUrl);
        claims.put("role", role);
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(openid)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * 从Token中获取微信OpenID
     * 
     * @param token JWT Token
     * @return 微信OpenID
     */
    public String getOpenidFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getSubject();
    }
    
    /**
     * 从Token中获取用户ID
     * 
     * @param token JWT Token
     * @return 用户ID
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        String userIdStr = (String) claims.get("userId");
        return UUID.fromString(userIdStr);
    }
    
    /**
     * 从Token中获取用户角色
     * 
     * @param token JWT Token
     * @return 用户角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get("role");
    }
    
    /**
     * 从Token中获取所有声明
     * 
     * @param token JWT Token
     * @return Claims对象
     */
    private Claims getAllClaimsFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * 验证Token是否有效
     * 
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            
            return true;
        } catch (SecurityException ex) {
            System.err.println("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            System.err.println("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            System.err.println("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            System.err.println("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            System.err.println("JWT claims string is empty");
        }
        return false;
    }
    
    /**
     * 获取Token过期时间
     * 
     * @param token JWT Token
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getExpiration();
    }
    
    /**
     * 检查Token是否过期
     * 
     * @param token JWT Token
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
}
