package id.rockierocker.imagetools.service;

import id.rockierocker.imagetools.constant.UserType;
import id.rockierocker.imagetools.dto.UserDetails;
import id.rockierocker.imagetools.exception.BadRequestException;
import id.rockierocker.imagetools.constant.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${redis.key.token-prefix:auth:token:}")
    private String tokenKeyPrefix;


    public UserDetails getCurrentUser() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BadRequestException(ResponseCode.UNAUTHORIZED);
        }
        HttpServletRequest request = attributes.getRequest();
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            throw new BadRequestException(ResponseCode.UNAUTHORIZED);
        }
        return getUserByToken(token);
    }

    public UserDetails getUserThrowIfHasError() {
        UserDetails userDetails = getCurrentUser();
        if (userDetails == null) {
            throw new BadRequestException(ResponseCode.UNAUTHORIZED);
        }
        return userDetails;
    }

    private UserDetails getUserByToken(String token) {
        String rawToken = extractToken(token);
        String redisKey = tokenKeyPrefix + rawToken;

        log.info("Getting user by token, redisKey={}", redisKey);

        String json = redisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.hasText(json)) {
            return UserDetails.builder()
                    .userId(rawToken)
                    .fullName("Anonymous")
                    .email("anonymous@mail.com")
                    .userType(UserType.ANONYMOUS)
                    .build();
        }
        UserDetails userDetails = objectMapper.readValue(json, UserDetails.class);
        log.info("User found: userId={}", userDetails.getUserId());
        return userDetails;
    }

    /**
     * Strip "Bearer " prefix jika ada.
     */
    private String extractToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BadRequestException(ResponseCode.UNAUTHORIZED);
        }
        if (token.startsWith("Bearer ") || token.startsWith("bearer ")) {
            return token.substring(7).trim();
        }
        return token.trim();
    }
}
