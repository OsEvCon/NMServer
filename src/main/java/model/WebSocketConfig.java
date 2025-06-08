package model;


import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); // Для broadcast-рассылки
        registry.setApplicationDestinationPrefixes("/app"); // Для приватных сообщений
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-updates")
                .setHandshakeHandler(new DefaultHandshakeHandler())
                .setAllowedOriginPatterns("*");


        // Альтернативный вариант для SockJS
        registry.addEndpoint("/ws-updates")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
