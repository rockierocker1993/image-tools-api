package id.rockierocker.imagetools.dto.websocket;

import id.rockierocker.imagetools.constant.Module;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class JobNotifyDto {
    private String requestId;
    private String webpUrl;
    private Module module;
    private boolean status;
}
