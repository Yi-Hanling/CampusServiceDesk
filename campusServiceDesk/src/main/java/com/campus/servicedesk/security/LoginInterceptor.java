package com.campus.servicedesk.security;

import com.campus.servicedesk.entity.SysUser;
import com.campus.servicedesk.mapper.SysUserMapper;
import com.campus.servicedesk.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 1. 放行非业务请求和登录注册
        if (!uri.startsWith("/api/")) return true;
        if (uri.equals("/api/auth/login") || uri.equals("/api/auth/register") || uri.equals("/api/health")) {
            return true;
        }

        // 2. 获取请求头中的 Token
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或Token缺失\",\"data\":null}");
            return false;
        }

        try {
            // 3. 解析 Token
            Claims claims = jwtUtil.parseToken(token.replace("Bearer ", ""));
            // 注意这里：用 toString 再转 Integer 最安全，防止 JWT 反序列化变成 Double 等类型
            Integer userId = Integer.valueOf(claims.get("userId").toString());

            // 4. 查库校验账号状态
            SysUser dbUser = sysUserMapper.selectById(userId);
            if (dbUser == null || dbUser.getStatus() == 0) {
                response.setStatus(403);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":403,\"message\":\"账号已失效，请重新登录\",\"data\":null}");
                return false;
            }

            // 5. 将最新的用户信息存入 request 域
            request.setAttribute("userId", userId);
            request.setAttribute("role", dbUser.getRole());

            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token无效或已过期\",\"data\":null}");
            return false;
        }
    }
}