package com.example.facility.identity.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.example.facility.identity.model.AuthType;
import com.example.facility.identity.model.User;
import com.example.facility.identity.model.UserRole;
import com.example.facility.identity.repository.UserRepository;

import java.util.Map;
import java.util.Random;

@Slf4j
@Service
public class OAuth2Service {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String EMAIL_PATTERN = "%s@example.com";
    private static final Random random = new Random();

    @Value("${keycloak.auth-server-url}")
    private String keycloakAuthServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    public OAuth2Service(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User authenticateViaOAuth2(String oauthProviderId, String username) {
        log.info("Authenticating user via OAuth2 with provider ID: {}", oauthProviderId);

        User existingUser = userRepository.findByOauthProviderId(oauthProviderId).orElse(null);
        if (existingUser != null) {
            log.info("User already exists with OAuth2 provider ID: {}", oauthProviderId);
            return existingUser;
        }

        User newUser = new User();
        newUser.setUsername(username != null ? username : generateRandomUsername());
        newUser.setEmail(generateMockedEmail());
        newUser.setPassword("");
        newUser.setRole(UserRole.MANAGER);
        newUser.setEnabled(true);
        newUser.setAuthType(AuthType.OAUTH2_KEYCLOAK);
        newUser.setOauthProviderId(oauthProviderId);

        log.info("Creating new OAuth2 user with email: {}", newUser.getEmail());
        return userRepository.save(newUser);
    }

    @SuppressWarnings("unchecked")
    public boolean validateKeycloakToken(String token) {
        log.info("Validating Keycloak token via introspection endpoint");
        try {
            String url = keycloakAuthServerUrl + "/realms/" + realm
                    + "/protocol/openid-connect/token/introspect";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("token", token);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Boolean.TRUE.equals(response.getBody().get("active"));
            }
            return false;
        } catch (Exception e) {
            log.error("Keycloak token introspection failed: {}", e.getMessage());
            return false;
        }
    }

    private String generateMockedEmail() {
        return String.format(EMAIL_PATTERN, generateRandomPrefix() + random.nextInt(1000));
    }

    private String generateRandomPrefix() {
        String letters = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            sb.append(letters.charAt(random.nextInt(letters.length())));
        }
        return sb.toString();
    }

    private String generateRandomUsername() {
        return "oauth2_user_" + System.currentTimeMillis();
    }
}

