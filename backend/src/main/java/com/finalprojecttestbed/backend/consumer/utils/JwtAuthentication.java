package com.finalprojecttestbed.backend.consumer.utils;

import com.finalprojecttestbed.backend.utils.JwtUtil;
import io.jsonwebtoken.Claims;

public class JwtAuthentication {
    public static String getUserId (String token) {
        String userId = null;
        try {
            Claims claims = JwtUtil.parseJWT(token);
            userId = claims.getSubject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return userId;
    }
}
