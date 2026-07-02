package com.platform.page.page.service;

import com.platform.common.tenant.TenantContext;
import com.platform.common.web.CursorPage;
import com.platform.page.exception.ResourceNotFoundException;
import com.platform.page.page.domain.PageDefinition;
import com.platform.page.page.domain.PageStatus;
import com.platform.page.page.dto.CreatePageRequest;
import com.platform.page.page.dto.PageDefinitionDto;
import com.platform.page.page.dto.PublishPageRequest;
import com.platform.page.page.dto.UpdatePageRequest;
import com.platform.page.page.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageService {

    private final PageRepository pageRepository;

    @Transactional
    public PageDefinitionDto createPage(CreatePageRequest req, String createdBy) {
        String tenantId = TenantContext.getTenantId();
        if (pageRepository.existsByTenantIdAndPageKey(tenantId, req.pageKey())) {
            throw new IllegalArgumentException("Page key already exists: " + req.pageKey());
        }

        PageDefinition page = PageDefinition.builder()
                .tenantId(tenantId)
                .pageKey(req.pageKey())
                .name(req.name())
                .description(req.description())
                .schema(req.schema())
                .status(PageStatus.DRAFT)
                .version(0)
                .createdBy(createdBy)
                .build();

        page = pageRepository.save(page);
        log.info("Created page pageKey={} tenantId={}", req.pageKey(), tenantId);
        return toDto(page);
    }

    public CursorPage<PageDefinitionDto> listPages(String cursor, int pageSize) {
        String tenantId = TenantContext.getTenantId();
        List<PageDefinition> results = cursor != null
                ? pageRepository.findAllByTenantIdAndIdGreaterThanOrderByIdAsc(tenantId, UUID.fromString(cursor))
                : pageRepository.findAllByTenantIdOrderByIdAsc(tenantId);

        List<PageDefinition> page = results.stream().limit(pageSize + 1L).toList();
        boolean hasMore = page.size() > pageSize;
        List<PageDefinition> items = hasMore ? page.subList(0, pageSize) : page;
        String nextCursor = hasMore ? items.get(items.size() - 1).getId().toString() : null;
        return CursorPage.of(items.stream().map(this::toDto).toList(), nextCursor, hasMore, pageSize);
    }

    public PageDefinitionDto getPage(String pageKey) {
        return toDto(requirePage(pageKey));
    }

    @Transactional
    public PageDefinitionDto updatePage(String pageKey, UpdatePageRequest req) {
        PageDefinition page = requirePage(pageKey);
        if (req.name() != null) page.setName(req.name());
        if (req.description() != null) page.setDescription(req.description());
        if (req.schema() != null) page.setSchema(req.schema());
        page = pageRepository.save(page);
        log.info("Updated page pageKey={}", pageKey);
        return toDto(page);
    }

    @Transactional
    public PageDefinitionDto publishPage(String pageKey, PublishPageRequest req, String publishedBy) {
        PageDefinition page = requirePage(pageKey);
        page.setSchema(req.schema());
        page.setVersion(page.getVersion() + 1);
        page.setStatus(PageStatus.PUBLISHED);
        page = pageRepository.save(page);
        log.info("Published page pageKey={} version={}", pageKey, page.getVersion());
        return toDto(page);
    }

    @Transactional
    public PageDefinitionDto deprecatePage(String pageKey) {
        PageDefinition page = requirePage(pageKey);
        page.setStatus(PageStatus.DEPRECATED);
        page = pageRepository.save(page);
        log.info("Deprecated page pageKey={}", pageKey);
        return toDto(page);
    }

    private PageDefinition requirePage(String pageKey) {
        return pageRepository.findByTenantIdAndPageKey(TenantContext.getTenantId(), pageKey)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found: " + pageKey));
    }

    private PageDefinitionDto toDto(PageDefinition p) {
        return new PageDefinitionDto(
                p.getId(),
                p.getTenantId(),
                p.getPageKey(),
                p.getName(),
                p.getDescription(),
                p.getSchema(),
                p.getStatus(),
                p.getVersion(),
                p.getCreatedBy(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
