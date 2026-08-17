package com.phraseforge.phraseforge_api.author;

import com.phraseforge.phraseforge_api.author.dto.AuthorResponse;
import com.phraseforge.phraseforge_api.author.dto.AuthorSummaryResponse;
import com.phraseforge.phraseforge_api.author.dto.CreateAuthorRequest;
import com.phraseforge.phraseforge_api.author.dto.UpdateAuthorRequest;
import com.phraseforge.phraseforge_api.common.PagedResponse;
import com.phraseforge.phraseforge_api.phrase.PhraseService;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseSummaryResponse;
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
@RequestMapping("/api/v1/authors")
public class AuthorController {

    private final AuthorService authorService;
    private final PhraseService phraseService;

    public AuthorController(AuthorService authorService, PhraseService phraseService) {
        this.authorService = authorService;
        this.phraseService = phraseService;
    }

    @GetMapping
    public PagedResponse<AuthorSummaryResponse> list(Pageable pageable) {
        return authorService.list(pageable);
    }

    @GetMapping("/{id}/phrases")
    public PagedResponse<PhraseSummaryResponse> phrasesByAuthor(
            @PathVariable Long id, Pageable pageable) {
        authorService.ensureExists(id);
        return phraseService.list(null, id, null, null, null, pageable);
    }

    @GetMapping("/{id}")
    public AuthorResponse getById(@PathVariable Long id) {
        return authorService.getById(id);
    }

    @PostMapping
    public ResponseEntity<AuthorResponse> create(@Valid @RequestBody CreateAuthorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authorService.create(request));
    }

    @PutMapping("/{id}")
    public AuthorResponse update(@PathVariable Long id,
                                 @Valid @RequestBody UpdateAuthorRequest request) {
        return authorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
