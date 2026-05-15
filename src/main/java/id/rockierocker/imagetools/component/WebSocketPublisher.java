package id.rockierocker.imagetools.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Component untuk mengirim pesan ke client via WebSocket (STOMP).
 *
 * <p>Pola penggunaan:
 * <pre>
 *   // Broadcast hasil job ke semua yang subscribe /topic/job/{requestId}
 *   webSocketPublisher.sendJobResult("abc-123", resultDto);
 *
 *   // Kirim notifikasi error
 *   webSocketPublisher.sendJobError("abc-123", "Processing failed");
 * </pre>
 *
 * <p>Client (JS) subscribe ke:
 * <pre>
 *   stompClient.subscribe('/topic/job/abc-123', callback);
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketPublisher {


//    @Component
//    public class WebSocketAuthInterceptor implements ChannelInterceptor {
//
//        @Override
//        public Message<?> preSend(Message<?> message, MessageChannel channel) {
//            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
//
//            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
//                // Ambil token dari connectHeaders
//                String authHeader = accessor.getFirstNativeHeader("Authorization");
//
//                if (authHeader != null && authHeader.startsWith("Bearer ")) {
//                    String token = authHeader.substring(7);
//                    // Validasi token JWT di sini
//                    // Set authentication ke security context
//                }
//            }
//            return message;
//        }
//    }
    private final SimpMessagingTemplate messagingTemplate;

    // Destination prefix untuk job result
    private static final String TOPIC_JOB = "/topic/job/";

    // Destination prefix untuk notifikasi error
    private static final String TOPIC_JOB_ERROR = "/topic/job-error/";

    /**
     * Kirim hasil job ke semua client yang subscribe channel userId tersebut.
     *
     * @param userId ID job (digunakan sebagai bagian dari destination)
     * @param payload   object hasil yang akan dikirim (di-serialize ke JSON otomatis)
     */
    public void sendJobResult(String userId, Object payload) {
        String destination = TOPIC_JOB + userId;
        log.info("Sending job result via WebSocket: destination={}", destination);
        try {
            messagingTemplate.convertAndSend(destination, payload);
            log.info("Job result sent via WebSocket: destination={}", destination);
        } catch (Exception e) {
            log.error("Failed to send job result via WebSocket: destination={}, error={}", destination, e.getMessage(), e);
            throw new RuntimeException("Failed to send WebSocket message to: " + destination, e);
        }
    }

    /**
     * Kirim notifikasi error job ke client yang subscribe.
     *
     * @param requestId ID job yang gagal
     * @param message   pesan error
     */
    public void sendJobError(String requestId, String message) {
        String destination = TOPIC_JOB_ERROR + requestId;
        log.warn("Sending job error via WebSocket: destination={}, message={}", destination, message);
        try {
            messagingTemplate.convertAndSend(destination, new JobErrorMessage(requestId, message));
            log.info("Job error sent via WebSocket: destination={}", destination);
        } catch (Exception e) {
            log.error("Failed to send job error via WebSocket: destination={}, error={}", destination, e.getMessage(), e);
        }
    }

    /**
     * Kirim pesan ke destination WebSocket yang bebas (custom destination).
     *
     * @param destination destination STOMP (contoh: /topic/custom/event)
     * @param payload     object yang akan dikirim
     */
    public void send(String destination, Object payload) {
        log.info("Sending WebSocket message: destination={}", destination);
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception e) {
            log.error("Failed to send WebSocket message: destination={}, error={}", destination, e.getMessage(), e);
            throw new RuntimeException("Failed to send WebSocket message to: " + destination, e);
        }
    }

    // -------------------------------------------------------------------------
    // Inner class
    // -------------------------------------------------------------------------

    /**
     * Payload standar untuk notifikasi error job via WebSocket.
     */
    public record JobErrorMessage(String requestId, String message) {}
}

