package id.rockierocker.imagetools.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Konfigurasi WebSocket menggunakan STOMP protocol.
 *
 * Endpoint  : /ws  (SockJS fallback tersedia)
 * Subscribe : /topic/job/{requestId}  → broadcast hasil job ke semua subscriber
 * Send      : /app/...               → dari client ke server (opsional)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.allowed-origins:*}")
    private String allowedOrigins;

    /**
     * Konfigurasi message broker.
     * - /topic : prefix untuk broadcast (server → client)
     * - /app   : prefix untuk pesan dari client ke server
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Aktifkan in-memory broker untuk prefix /topic dan /queue
        registry.enableSimpleBroker("/topic", "/queue");
        // Prefix untuk pesan yang diarahkan ke @MessageMapping method
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Daftarkan endpoint WebSocket.
     * Client connect ke: ws://host/image-tools/api/ws
     * Atau via SockJS  : http://host/image-tools/api/ws
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS(); // fallback untuk browser yang tidak support WebSocket
    }
}

