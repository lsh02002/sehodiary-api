package com.shop.sehodiary_api.config.filter;

import com.shop.sehodiary_api.config.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = request.getHeader("accessToken");
        String refreshToken = request.getHeader("refreshToken");

        if (jwtTokenProvider.validateToken(accessToken)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else if (refreshToken != null) {
            if (jwtTokenProvider.validateRefreshToken(refreshToken)) {
                String email = jwtTokenProvider.getEmail(refreshToken);
                String newAccessToken = jwtTokenProvider.createAccessToken(email);
                response.addHeader("accessToken", newAccessToken);
                Authentication authentication = jwtTokenProvider.getAuthentication(newAccessToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
