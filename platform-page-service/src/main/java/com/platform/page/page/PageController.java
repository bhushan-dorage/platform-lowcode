package com.platform.page.page;

import com.platform.common.web.CursorPage;
import com.platform.common.web.StandardResponseEnvelope;
import com.platform.page.page.dto.CreatePageRequest;
import com.platform.page.page.dto.GeneratePageRequest;
import com.platform.page.page.dto.GeneratedPageResponse;
import com.platform.page.page.dto.PageDefinitionDto;
import com.platform.page.page.dto.PublishPageRequest;
import com.platform.page.page.dto.UpdatePageRequest;
import com.platform.page.page.service.PageGenerationService;
import com.platform.page.page.service.PageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pages")
@RequiredArgsConstructor
public class PageController {

    private final PageService pageService;
    private final PageGenerationService pageGenerationService;

    @PostMapping("/generate")
    public ResponseEntity<StandardResponseEnvelope<GeneratedPageResponse>> generatePage(
            @Valid @RequestBody GeneratePageRequest req,
            HttpServletRequest http) {
        return ResponseEntity.ok(ok(pageGenerationService.generate(req), http));
    }

    @PostMapping
    public ResponseEntity<StandardResponseEnvelope<PageDefinitionDto>> createPage(
            @Valid @RequestBody CreatePageRequest req,
            Authentication auth,
            HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ok(pageService.createPage(req, auth.getName()), http));
    }

    @GetMapping
    public ResponseEntity<StandardResponseEnvelope<CursorPage<PageDefinitionDto>>> listPages(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest http) {
        return ResponseEntity.ok(ok(pageService.listPages(cursor, pageSize), http));
    }

    @GetMapping("/{pageKey}")
    public ResponseEntity<StandardResponseEnvelope<PageDefinitionDto>> getPage(
            @PathVariable String pageKey,
            HttpServletRequest http) {
        return ResponseEntity.ok(ok(pageService.getPage(pageKey), http));
    }

    @PutMapping("/{pageKey}")
    public ResponseEntity<StandardResponseEnvelope<PageDefinitionDto>> updatePage(
            @PathVariable String pageKey,
            @RequestBody UpdatePageRequest req,
            HttpServletRequest http) {
        return ResponseEntity.ok(ok(pageService.updatePage(pageKey, req), http));
    }

    @PostMapping("/{pageKey}/publish")
    public ResponseEntity<StandardResponseEnvelope<PageDefinitionDto>> publishPage(
            @PathVariable String pageKey,
            @Valid @RequestBody PublishPageRequest req,
            Authentication auth,
            HttpServletRequest http) {
        return ResponseEntity.ok(ok(pageService.publishPage(pageKey, req, auth.getName()), http));
    }

    @DeleteMapping("/{pageKey}")
    public ResponseEntity<Void> deprecatePage(
            @PathVariable String pageKey,
            HttpServletRequest http) {
        pageService.deprecatePage(pageKey);
        return ResponseEntity.noContent().build();
    }

    private <T> StandardResponseEnvelope<T> ok(T data, HttpServletRequest req) {
        Object rid = req.getAttribute("requestId");
        return StandardResponseEnvelope.of(
                data,
                rid != null ? rid.toString() : MDC.get("requestId"),
                MDC.get("traceId"));
    }
}
