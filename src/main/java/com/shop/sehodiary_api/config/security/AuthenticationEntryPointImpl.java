package com.shop.sehodiary_api.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        if(request.getHeader("accessToken") != null){
            response.sendRedirect("/user/entrypoint?accessToken=" + request.getHeader("accessToken"));
        }else{
            response.sendRedirect("/user/entrypoint");
        }
    }
}
