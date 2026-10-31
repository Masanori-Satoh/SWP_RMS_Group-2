package com.group2.rms.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 * TEST: bộ chặn kênh WebSocket chỉ cho người đã đăng nhập nghe kênh riêng của mình.
 */
class WebSocketConfigTests {

    private final WebSocketConfig.OwnQueueOnly interceptor = new WebSocketConfig.OwnQueueOnly();
    private final Principal hm = new UsernamePasswordAuthenticationToken("hm", null);

    private static Message<byte[]> frame(StompCommand command, String destination, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(user);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("Đã đăng nhập nghe /user/queue/notifications -> Cho qua")
    void preSend_shouldAllow_whenSubscribingOwnQueue() {
        Message<byte[]> message = frame(StompCommand.SUBSCRIBE, "/user/queue/notifications", hm);

        assertSame(message, interceptor.preSend(message, null));
    }

    @Test
    @DisplayName("Nghe thẳng /queue/... (kênh phiên khác) -> Chặn")
    void preSend_shouldReject_whenSubscribingRawQueue() {
        Message<byte[]> message = frame(StompCommand.SUBSCRIBE, "/queue/notifications-userabc123", hm);

        assertThrows(MessagingException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    @DisplayName("Chưa đăng nhập nghe kênh riêng -> Chặn")
    void preSend_shouldReject_whenAnonymous() {
        Message<byte[]> message = frame(StompCommand.SUBSCRIBE, "/user/queue/notifications", null);

        assertThrows(MessagingException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    @DisplayName("Trình duyệt gửi tin lên (SEND) -> Chặn")
    void preSend_shouldReject_whenClientSends() {
        Message<byte[]> message = frame(StompCommand.SEND, "/user/queue/notifications", hm);

        assertThrows(MessagingException.class, () -> interceptor.preSend(message, null));
    }
}
