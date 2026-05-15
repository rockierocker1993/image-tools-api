package id.rockierocker.imagetools.consumer;

import id.rockierocker.imagetools.dto.ConsumerRequest;
import id.rockierocker.imagetools.dto.RembgResponseDto;
import id.rockierocker.imagetools.service.job.RembgJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Redis Pub/Sub subscriber untuk channel "job-rembg".
 * Didaftarkan ke {@code RedisMessageListenerContainer} via {@code RedisConfig}.
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class JobRembgConsumer implements MessageListener {

    private final RembgJobService rembgJobService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String key = new String(message.getBody());
        String data = redisTemplate.opsForValue().get(key);
        log.info("Received Redis message on channel={} with key={}", channel, key);
        if (data == null) {
            // sudah expired
            log.warn("Message expired for key={}", key);
            return;
        }

        try {
            ConsumerRequest<RembgResponseDto> jobMessage = objectMapper.readValue(data, new TypeReference<ConsumerRequest<RembgResponseDto>>() {
            });
            rembgJobService.consumeJobResult(jobMessage);
            log.info("Job [{}] processed successfully", jobMessage.getRequestId());
        } catch (Exception e) {
            log.error("Failed to process Redis message on channel={} : {}", channel, e.getMessage(), e);
        }
    }

}
