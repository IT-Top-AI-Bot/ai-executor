package com.aquadev.aiexecutor.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class TelegramUserIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Telegram-User-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String raw = request.getHeader(HEADER);
        if (raw == null || raw.isBlank()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing header: " + HEADER);
            return;
        }

        long telegramUserId;
        try {
            telegramUserId = Long.parseLong(raw);
            if (telegramUserId <= 0) throw new NumberFormatException("must be > 0");
        } catch (NumberFormatException _) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid header: " + HEADER);
            return;
        }

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(telegramUserId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator") || uri.startsWith("/error");
    }
}
