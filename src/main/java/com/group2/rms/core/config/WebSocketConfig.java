package com.group2.rms.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/*
 * CONFIG: WebSocket (STOMP) để server đẩy thông báo; trình duyệt chỉ được nghe kênh riêng của mình (/user/queue/...).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    public static final String ENDPOINT = "/ws";

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(ENDPOINT);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new OwnQueueOnly());
    }

    /** Chỉ cho nghe kênh riêng (/user/...); không cho gửi lên, không cho nghe thẳng kênh của phiên khác. */
    static class OwnQueueOnly implements ChannelInterceptor {

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            if (StompCommand.SEND.equals(accessor.getCommand())) {
                throw new MessagingException("Không hỗ trợ gửi tin qua WebSocket.");
            }
            if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                    && (accessor.getUser() == null || accessor.getDestination() == null
                    || !accessor.getDestination().startsWith("/user/"))) {
                throw new MessagingException("Chỉ được nghe kênh thông báo của chính mình.");
            }
            return message;
        }
    }
}
