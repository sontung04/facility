package com.example.facility.identity.service;

import com.example.facility.shared.config.TokenBlacklist;
import com.example.facility.shared.exception.ErrorCode;
import com.example.facility.shared.exception.WebException;
import com.example.facility.identity.dto.request.LoginRequest;
import com.example.facility.identity.dto.request.RefreshTokenRequest;
import com.example.facility.identity.dto.request.RegisterRequest;
import com.example.facility.identity.dto.response.AuthResponse;
import com.example.facility.identity.model.RefreshToken;
import com.example.facility.identity.model.User;
import com.example.facility.identity.repository.RefreshTokenRepository;
import com.example.facility.identity.repository.UserRepository;
import com.example.facility.identity.util.JwtProvider;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationService {

    private final BasicAuthService basicAuthService;
    private final OAuth2Service oAuth2Service;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklist tokenBlacklist;

    public AuthResponse register(@Valid RegisterRequest request) {
        log.info("Registering new user with auth type: {}", request.getAuthType());

        if (request.getAuthType() == null) {
            throw new WebException(ErrorCode.AUTH_TYPE_REQUIRED);
        }

        if ("BASIC_AUTH".equalsIgnoreCase(request.getAuthType())) {
            String adminUsername = resolveCallerUsername();
            if (adminUsername == null) {
                throw new WebException(ErrorCode.ADMIN_ONLY_OPERATION);
            }
            User user = basicAuthService.registerBasicAuth(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getRole(),
                    adminUsername);
            return buildAuthResponse(user);
        } else if ("OAUTH2_KEYCLOAK".equalsIgnoreCase(request.getAuthType())) {
            throw new WebException(ErrorCode.OAUTH2_AUTO_REGISTRATION);
        } else {
            throw new WebException(ErrorCode.INVALID_AUTH_TYPE);
        }
    }

    public AuthResponse login(@Valid LoginRequest request) {
        log.info("User login attempt with auth type: {}", request.getAuthType());

        if (request.getAuthType() == null) {
            request.setAuthType("BASIC_AUTH");
        }

        User user;

        if ("BASIC_AUTH".equalsIgnoreCase(request.getAuthType())) {
            user = basicAuthService.authenticateBasicAuth(request.getUsername(), request.getPassword());
        } else if ("OAUTH2_KEYCLOAK".equalsIgnoreCase(request.getAuthType())) {
            String oauthProviderId = request.getUsername();
            if (!oAuth2Service.validateKeycloakToken(oauthProviderId)) {
                throw new WebException(ErrorCode.INVALID_KEYCLOAK_TOKEN);
            }
            user = oAuth2Service.authenticateViaOAuth2(oauthProviderId, request.getUsername());
        } else {
            throw new WebException(ErrorCode.INVALID_AUTH_TYPE);
        }

        log.info("User logged in successfully: {}", user.getUsername());
        return buildAuthResponse(user);
    }

    public AuthResponse refresh(@Valid RefreshTokenRequest request) {
        log.info("Refreshing access token");

        String refreshToken = request.getRefreshToken();

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new WebException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String username = jwtProvider.getUsernameFromToken(refreshToken);
        if (username == null) {
            throw new WebException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new WebException(ErrorCode.USER_NOT_FOUND));

        RefreshToken stored = refreshTokenRepository.findByUserId(user.getId())
                .orElseThrow(() -> new WebException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (!stored.getTokenHash().equals(sha256(refreshToken))) {
            throw new WebException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.deleteByUserId(user.getId());
            throw new WebException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // Rotate: delete old, issue new pair
        refreshTokenRepository.deleteByUserId(user.getId());
        return buildAuthResponse(user);
    }

    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            if (jwtProvider.validateToken(token)) {
                Claims claims = jwtProvider.getClaimsFromToken(token);
                String jti = claims.getId();
                Date expiration = claims.getExpiration();

                if (jti != null) {
                    long ttl = expiration.getTime() - System.currentTimeMillis();
                    tokenBlacklist.revoke(jti, ttl);
                }

                String username = claims.getSubject();
                if (username != null) {
                    userRepository.findByUsername(username)
                            .ifPresent(user -> refreshTokenRepository.deleteByUserId(user.getId()));
                }
            }
        } catch (Exception e) {
            log.error("Error processing logout: {}", e.getMessage());
        }

        log.info("User logged out successfully");
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtProvider.generateAccessToken(
                user.getUsername(),
                user.getEmail(),
                user.getRole().toString(),
                user.getAuthType().toString());

        String refreshToken = jwtProvider.generateRefreshToken(user.getUsername());

        Claims refreshClaims = jwtProvider.getClaimsFromToken(refreshToken);
        LocalDateTime expiresAt = refreshClaims.getExpiration().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        refreshTokenRepository.deleteByUserId(user.getId());

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(sha256(refreshToken))
                .expiresAt(expiresAt)
                .build());

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(86400000L);
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().toString());
        response.setAuthType(user.getAuthType().toString());

        return response;
    }

    private String resolveCallerUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return (String) auth.getPrincipal();
        }
        return null;
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}

