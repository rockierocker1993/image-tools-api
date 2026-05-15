package id.rockierocker.imagetools.service;

import id.rockierocker.imagetools.constant.ResponseCode;
import id.rockierocker.imagetools.dto.BaseResponse;
import id.rockierocker.imagetools.dto.FaqListResponseDto;
import id.rockierocker.imagetools.repository.FaqRepository;
import id.rockierocker.imagetools.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class FaqService {

    private final String faqCacheKeyPrefix = "FAQ:";
    private final FaqRepository faqRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public ResponseEntity<BaseResponse<List<FaqListResponseDto>>> getFaqList(String category) {
        log.info("Get FAQ List");
        String cacheKey = faqCacheKeyPrefix + category;
        String cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            try {
                List<FaqListResponseDto> faqList = objectMapper.readValue(cachedData, objectMapper.getTypeFactory().constructCollectionType(List.class, FaqListResponseDto.class));
                log.info("FAQ List retrieved from cache: {}", faqList.size());
                return ResponseEntity.ok().body(ResponseUtil.buildSuccessResponse(ResponseCode.SUCCESS, faqList));
            } catch (Exception e) {
                log.error("Failed to parse cached FAQ data for category {}: {}", category, e.getMessage(), e);
            }
        }
        List<FaqListResponseDto> faqList = faqRepository.findByCategoryOrderBySortOrderAsc(category).stream()
                .map(faq -> new FaqListResponseDto(faq.getQuestion(), faq.getAnswer()))
                .toList();
        redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(faqList), Duration.ofSeconds(60*60)); // cache selama 1 jam
        log.info("FAQ List retrieved: {}", faqList.size());
        return ResponseEntity.ok().body(ResponseUtil.buildSuccessResponse(ResponseCode.SUCCESS, faqList));
    }
}
