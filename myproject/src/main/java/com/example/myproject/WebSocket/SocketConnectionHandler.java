package com.example.myproject.WebSocket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.example.myproject.Repositories.RedisRepository;

@Component
public class SocketConnectionHandler extends TextWebSocketHandler {
    List<WebSocketSession> webSocketSessions = Collections.synchronizedList(new ArrayList<>());
    
    
    private RedisRepository redisRepository;
    
    public SocketConnectionHandler(RedisRepository redisRepository){
        this.redisRepository = redisRepository;
    }


    // метод подключения 
    @Override 
    public void afterConnectionEstablished(WebSocketSession session) throws Exception{
        super.afterConnectionEstablished(session);



        redisRepository.setUserOnline(getEmail(session), true);
        
        
        System.out.println(session.getId() + " connected.");
       
        webSocketSessions.add(session);

        sendMessage();

        
    }

    // метод отключения 
    @Override 
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        redisRepository.setUserOnline(getEmail(session), false);
        System.out.println(session.getId() + " disconnected.");

        webSocketSessions.remove(session);
        sendMessage();
    }

    private String getEmail(WebSocketSession session){
        Object email = session.getAttributes().get("email");
        if (email != null){
            return email.toString();
        }   
        else{
            return "";
        }
    }

    


    private void sendMessage(){
        long couter = redisRepository.countOnlineUsers();
        TextMessage message = new TextMessage(String.valueOf(couter));
        synchronized (webSocketSessions){
            for (WebSocketSession s: webSocketSessions){
                try{
                    s.sendMessage(message);
                }catch (Exception e){
                    System.err.println("Ошибка: " + e);
                }
            }
        }
    }
  
}