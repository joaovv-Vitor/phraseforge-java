package com.phraseforge.phraseforge_api.tag;

import com.phraseforge.phraseforge_api.common.PagedResponse;
import com.phraseforge.phraseforge_api.tag.dto.CreateTagRequest;
import com.phraseforge.phraseforge_api.tag.dto.TagResponse;
import com.phraseforge.phraseforge_api.tag.dto.UpdateTagRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public PagedResponse<TagResponse> list(Pageable pageable) {
        return tagService.list(pageable);
    }

    @GetMapping("/{id}")
    public TagResponse getById(@PathVariable Long id) {
        return tagService.getById(id);
    }

    @PostMapping
    public ResponseEntity<TagResponse> create(@Valid @RequestBody CreateTagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.create(request));
    }

    @PutMapping("/{id}")
    public TagResponse update(@PathVariable Long id,
                              @Valid @RequestBody UpdateTagRequest request) {
        return tagService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
