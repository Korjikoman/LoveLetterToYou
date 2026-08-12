package com.example.myproject.WebSocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;


@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final UserHandshakeInterceptor interceptor;
    private final SocketConnectionHandler handler;

    public WebSocketConfig(
        UserHandshakeInterceptor interceptor,
        SocketConnectionHandler handler
    ){
        this.handler = handler;
        this.interceptor = interceptor;
    }


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){
        registry.addHandler(handler, "/ws").addInterceptors(interceptor).setAllowedOriginPatterns("*");
        // * - любой домен может подключаться к этому параметру
    }


}
