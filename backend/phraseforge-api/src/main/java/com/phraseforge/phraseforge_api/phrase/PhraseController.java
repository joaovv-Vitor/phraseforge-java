package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.common.PagedResponse;
import com.phraseforge.phraseforge_api.phrase.dto.CreatePhraseRequest;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseResponse;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseSummaryResponse;
import com.phraseforge.phraseforge_api.phrase.dto.UpdatePhraseRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/phrases")
public class PhraseController {

    private final PhraseService phraseService;

    public PhraseController(PhraseService phraseService) {
        this.phraseService = phraseService;
    }

    @GetMapping
    public PagedResponse<PhraseSummaryResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String language,
            Pageable pageable) {
        return phraseService.list(query, authorId, categoryId, tagId, language, pageable);
    }

    @GetMapping("/random")
    public PhraseResponse random() {
        return phraseService.random();
    }

    @GetMapping("/{id}")
    public PhraseResponse getById(@PathVariable Long id) {
        return phraseService.getById(id);
    }

    @PostMapping
    public ResponseEntity<PhraseResponse> create(@Valid @RequestBody CreatePhraseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(phraseService.create(request));
    }

    @PutMapping("/{id}")
    public PhraseResponse update(@PathVariable Long id,
                                 @Valid @RequestBody UpdatePhraseRequest request) {
        return phraseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        phraseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
