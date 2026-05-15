package id.rockierocker.imagetools.controller;

import id.rockierocker.imagetools.dto.SvgVectorRequestDto;
import id.rockierocker.imagetools.service.SvgVectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RequiredArgsConstructor
@RestController
@RequestMapping("/svg-vector")
public class SvgVectorController {

    private final SvgVectorService svgVectorService;

    @PostMapping(path = "/trace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> vtrace(@RequestParam("file") MultipartFile file, @ModelAttribute SvgVectorRequestDto traceDto)  {
        return svgVectorService.trace(file, traceDto);
    }

}
