package id.rockierocker.imagetools.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class RembgResponseDto {

    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("status")
    private String status;

    @JsonProperty("format")
    private String format;

    @JsonProperty("original_size")
    private List<Integer> originalSize;

    @JsonProperty("output_format")
    private String outputFormat;

    @JsonProperty("output_quality")
    private Integer outputQuality;

    @JsonProperty("output_size")
    private List<Integer> outputSize;

    @JsonProperty("output_url")
    private String outputUrl;

    @JsonProperty("output_volume")
    private String outputVolume;

    @JsonProperty("processing_time")
    private Double processingTime;

    @JsonProperty("model")
    private String model;

    @JsonProperty("input_storage_mode")
    private String inputStorageMode;

    @JsonProperty("output_storage_mode")
    private String outputStorageMode;

    @JsonProperty("webhook_triggered_at")
    private OffsetDateTime webhookTriggeredAt;
}
