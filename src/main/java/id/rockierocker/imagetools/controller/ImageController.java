package id.rockierocker.imagetools.controller;

import id.rockierocker.imagetools.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/image")
public class ImageController {

    private final ImageService imageService;

    @GetMapping(path = "/{imageId}")
    public ResponseEntity<?> getImageById(@PathVariable String imageId) {
        return imageService.getImageById(imageId);
    }

    @GetMapping(path = "/time-limit/{imageId}")
    public ResponseEntity<?> getImageTimeLimitById(@PathVariable String imageId) {
        return imageService.getImageTimeLimitById(imageId);
    }

}
