package com.yantai.superinventory.filter;

import com.yantai.superinventory.util.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 认证过滤器
 * 拦截所有请求，验证JWT Token有效性
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    // 白名单：不需要验证token的路径
    private static final List<String> WHITELIST = Arrays.asList(
        "/api/auth/wechat/login",
        "/api/auth/wechat/callback",
        "/api/auth/wx_verify",
        "/api/auth/logout"
    );
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // 检查是否在白名单中
        if (isWhitelisted(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 获取 Authorization header
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                // 验证 token
                if (jwtTokenProvider.validateToken(token)) {
                    // Token有效，继续处理请求
                    // 可以在这里将用户信息存入 SecurityContext 或 request attribute
                    String openid = jwtTokenProvider.getOpenidFromToken(token);
                    request.setAttribute("openid", openid);
                    request.setAttribute("userId", jwtTokenProvider.getUserIdFromToken(token));
                    request.setAttribute("role", jwtTokenProvider.getRoleFromToken(token));
                    
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (Exception e) {
                // Token无效
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"Token无效或已过期\"}");
                return;
            }
        }
        
        // 如果没有token或token无效，返回401
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"未授权访问，请先登录\"}");
    }
    
    /**
     * 检查路径是否在白名单中
     * 
     * @param path 请求路径
     * @return 是否在白名单中
     */
    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(path::startsWith);
    }
}
