package id.rockierocker.imagetools.controller;

import id.rockierocker.imagetools.service.FaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/faq")
public class FaqController {

    private final FaqService faqService;

    @GetMapping(path = "/category/{category}")
    public ResponseEntity<?> getFaqByCategory(@PathVariable String category) {
        return faqService.getFaqList(category);
    }

}
