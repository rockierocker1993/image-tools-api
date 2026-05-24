package id.rockierocker.imagetools.controller.tools;

import id.rockierocker.imagetools.service.tools.SvgVectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RequiredArgsConstructor
@RestController
@RequestMapping("/tools/svg-vector")
public class SvgVectorController {

    private final SvgVectorService svgVectorService;

    @PostMapping(path = "/trace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> trace(@RequestParam("file") MultipartFile file)  {
        return svgVectorService.trace(file);
    }

}
