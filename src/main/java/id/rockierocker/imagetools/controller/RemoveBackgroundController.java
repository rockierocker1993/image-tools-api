package id.rockierocker.imagetools.controller;

import id.rockierocker.imagetools.dto.removebackground.RemoveDto;
import id.rockierocker.imagetools.service.RemoveBackgroundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/remove-background")
public class RemoveBackgroundController {

    private final RemoveBackgroundService removeBackgroundService;

    @PostMapping(path = "/remove", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> remove(@RequestParam("file") MultipartFile file, @ModelAttribute RemoveDto removeDto)  {
        return removeBackgroundService.removeBackground(file, removeDto);
    }
}
