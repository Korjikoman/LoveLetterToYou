package com.example.myproject.WebSocket;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class UserHandshakeInterceptor implements HandshakeInterceptor {


    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null){
            return false;
        }

        if (authentication != null && authentication.isAuthenticated()){
            String email = authentication.getName();
            attributes.put("email", email);
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest arg0, ServerHttpResponse arg1, WebSocketHandler arg2,
            @Nullable Exception arg3) {
        
    }

    
}
