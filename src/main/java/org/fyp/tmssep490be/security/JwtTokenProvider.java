package org.fyp.tmssep490be.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityInMs;
    private final long refreshTokenValidityInMs;

    public JwtTokenProvider(
            @Value("${spring.security.jwt.secret:ThisIsAVerySecretKeyForJWTTokenGenerationPleaseChangeInProduction123456789}") String secret,
            @Value("${spring.security.jwt.access-token-expiration:900000}") long accessTokenValidityInMs,
            @Value("${spring.security.jwt.refresh-token-expiration:604800000}") long refreshTokenValidityInMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityInMs = accessTokenValidityInMs;
        this.refreshTokenValidityInMs = refreshTokenValidityInMs;
    }

    // Generate access token from authentication
    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidityInMs);

        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", ""))
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .claim("userId", userPrincipal.getId())
                .claim("email", userPrincipal.getEmail())
                .claim("roles", roles)
                .claim("type", "access")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateAccessToken(Long userId, String email, String roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidityInMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("email", email)
                .claim("roles", roles)
                .claim("type", "access")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken(Long userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenValidityInMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public Long getUserIdFromJwt(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("userId", Long.class);
    }

    public String getEmailFromJwt(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public String getTokenType(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("type", String.class);
    }

    public Date getExpirationDate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getExpiration();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }

    public boolean validateAccessToken(String token) {
        if (!validateToken(token)) {
            return false;
        }
        try {
            String tokenType = getTokenType(token);
            return "access".equals(tokenType);
        } catch (Exception e) {
            log.error("Error validating access token type", e);
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        if (!validateToken(token)) {
            return false;
        }
        try {
            String tokenType = getTokenType(token);
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            log.error("Error validating refresh token type", e);
            return false;
        }
    }

    public long getAccessTokenExpirationInSeconds() {
        return accessTokenValidityInMs / 1000;
    }

    // Tạo token đặt lại mật khẩu với thời hạn ngắn (dùng lại accessTokenValidityInMs ~ 15 phút)
    public String generatePasswordResetToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidityInMs);

        return Jwts.builder()
                .setSubject("password-reset")
                .claim("userId", userId)
                .claim("type", "password-reset")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    // Xác thực token đặt lại mật khẩu và trả về userId, null nếu không hợp lệ/hết hạn
    public Long validatePasswordResetToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get("type", String.class);
            if (!"password-reset".equals(tokenType)) {
                log.warn("Loại token không hợp lệ cho đặt lại mật khẩu: {}", tokenType);
                return null;
            }

            return claims.get("userId", Long.class);

        } catch (ExpiredJwtException ex) {
            log.warn("Token đặt lại mật khẩu đã hết hạn");
            return null;
        } catch (Exception ex) {
            log.error("Token đặt lại mật khẩu không hợp lệ", ex);
            return null;
        }
    }
}
