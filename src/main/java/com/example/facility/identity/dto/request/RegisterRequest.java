package com.example.facility.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Auth type is required")
    private String authType; // "BASIC_AUTH" or "OAUTH2_KEYCLOAK"

    private String oauthProviderId; // optional for OAuth2

    private String role; // optional; admin-assignable role (MANAGER or TECHNICIAN); defaults to MANAGER
}

