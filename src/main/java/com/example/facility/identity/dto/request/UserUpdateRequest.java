package com.example.facility.identity.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    private String role;     // MANAGER | TECHNICIAN | ADMIN — null = keep current
    private Boolean enabled; // null = keep current
}

