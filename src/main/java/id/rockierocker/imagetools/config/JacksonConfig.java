package id.rockierocker.imagetools.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.introspect.VisibilityChecker;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                // ✅ Tidak throw error saat object tidak punya property yg bisa di-serialize
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                // ✅ Ignore unknown JSON fields saat deserialisasi
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // ✅ Izinkan field private ter-serialize via Lombok @Data getter/setter
                .changeDefaultVisibility(vc -> vc
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                        .withGetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                        .withSetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                        .withIsGetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY))
                .build();
    }
}
