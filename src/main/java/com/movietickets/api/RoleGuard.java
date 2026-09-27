package com.movietickets.api;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RoleGuard {

    public void requireAdmin(String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }

    public void requireCustomer(String role) {
        if (!"CUSTOMER".equalsIgnoreCase(role)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "Customer role required");
        }
    }
}
