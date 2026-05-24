package id.rockierocker.imagetools.controller.tools;

import id.rockierocker.imagetools.dto.JobRequestDto;
import id.rockierocker.imagetools.service.tools.job.RembgJobService;
import id.rockierocker.imagetools.service.tools.job.UpscalerJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tools/job")
public class JobController {
    private final RembgJobService rembgJobService;
    private final UpscalerJobService upscalerJobService;

    @PostMapping(path = "/create-job/rembg", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crateJobRembg(@RequestParam("file") MultipartFile file) {
        return rembgJobService.crateJob(file, null);
    }

    @GetMapping(path = "/warming-up/rembg")
    public ResponseEntity<?> warmingUpRembg() {
        return rembgJobService.warmingUp();
    }

    @PostMapping(path = "/create-job/upscale", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crateJobUpscaler(@RequestParam("file") MultipartFile file, @ModelAttribute JobRequestDto jobRequestDto) {
        return upscalerJobService.crateJob(file, jobRequestDto);
    }

    @GetMapping(path = "/warming-up/upscale")
    public ResponseEntity<?> warmingUpUpscale() {
        return upscalerJobService.warmingUp();
    }

}
