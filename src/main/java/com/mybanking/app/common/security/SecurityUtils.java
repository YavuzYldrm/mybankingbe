package com.mybanking.app.common.security;

import com.mybanking.app.common.error.AppException;
import com.mybanking.app.common.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;

@Slf4j
public final class SecurityUtils {
    private static final String ANONYMOUS = "anonymousUser";

    private SecurityUtils() {}

    public static Authentication auth() {
        return getContext().getAuthentication();
    }

    public static String currentUserIdAsString() {
        Authentication a = auth();
        if (a == null || !a.isAuthenticated() || a.getName() == null || ANONYMOUS.equals(a.getName())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, UNAUTHORIZED, "Client is unauthorized");
        }
        return a.getName();
    }

    public static UUID currentUserId() {
        String id = currentUserIdAsString();
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.UNAUTHORIZED, UNAUTHORIZED, "Invalid user id in token");
        }
    }

    public static void requireAdmin() {
        Authentication a = auth();
        if (a == null || !a.isAuthenticated() || ANONYMOUS.equals(a.getName())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, UNAUTHORIZED, "Client is unauthorized");
        }
        boolean isAdmin = a.getAuthorities().stream()
                .anyMatch(granted -> "ROLE_ADMIN".equals(granted.getAuthority()));
        if (!isAdmin) {
            throw new AppException(ErrorCode.AUTH_FORBIDDEN, FORBIDDEN, "Admin required");
        }
    }
}
