package com.example.facility.shared.util;

import com.example.facility.identity.IdentityApi;
import com.example.facility.shared.exception.ErrorCode;
import com.example.facility.shared.exception.WebException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final IdentityApi identityApi;

    /** Returns the DB ID of the currently authenticated user. */
    public Long getCurrentUserId() {
        return identityApi.getUserIdByUsername(getCurrentUsername());
    }

    /** Returns the username from the Spring Security context. */
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new WebException(ErrorCode.FORBIDDEN);
        }
        return (String) auth.getPrincipal();
    }

    /** Returns the caller's role name without the {@code ROLE_} prefix (e.g. {@code "ADMIN"}). */
    public String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new WebException(ErrorCode.FORBIDDEN);
        }
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElseThrow(() -> new WebException(ErrorCode.FORBIDDEN));
    }
}
