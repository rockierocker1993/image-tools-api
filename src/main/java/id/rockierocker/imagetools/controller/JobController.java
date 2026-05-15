package id.rockierocker.imagetools.controller;

import id.rockierocker.imagetools.service.job.RembgJobService;
import id.rockierocker.imagetools.service.job.UpscalerJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/job")
public class JobController {
    private final RembgJobService rembgJobService;
    private final UpscalerJobService upscalerJobService;

    @PostMapping(path = "/create-job/rembg", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crateJobRembg(@RequestParam("file") MultipartFile file, @RequestHeader("Authorization") String bearerToken) {
        return rembgJobService.crateJob(file, bearerToken);
    }

    @GetMapping(path = "/warming-up/rembg")
    public ResponseEntity<?> warmingUpRembg() {
        return rembgJobService.warmingUp();
    }

    @PostMapping(path = "/create-job/upscale", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crateJobUpscaler(@RequestParam("file") MultipartFile file, @RequestHeader("Authorization") String bearerToken) {
        return upscalerJobService.crateJob(file, bearerToken);
    }

    @GetMapping(path = "/warming-up/upscale")
    public ResponseEntity<?> warmingUpUpscale() {
        return upscalerJobService.warmingUp();
    }
}
