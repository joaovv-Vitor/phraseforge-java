package com.phraseforge.phraseforge_api.category;

import com.phraseforge.phraseforge_api.category.dto.CategoryResponse;
import com.phraseforge.phraseforge_api.category.dto.CategorySummaryResponse;
import com.phraseforge.phraseforge_api.category.dto.CreateCategoryRequest;
import com.phraseforge.phraseforge_api.category.dto.UpdateCategoryRequest;
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
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final PhraseService phraseService;

    public CategoryController(CategoryService categoryService, PhraseService phraseService) {
        this.categoryService = categoryService;
        this.phraseService = phraseService;
    }

    @GetMapping
    public PagedResponse<CategorySummaryResponse> list(Pageable pageable) {
        return categoryService.list(pageable);
    }

    @GetMapping("/{id}/phrases")
    public PagedResponse<PhraseSummaryResponse> phrasesByCategory(
            @PathVariable Long id, Pageable pageable) {
        return phraseService.list(null, null, id, null, null, pageable);
    }

    @GetMapping("/{id}")
    public CategoryResponse getById(@PathVariable Long id) {
        return categoryService.getById(id);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id,
                                   @Valid @RequestBody UpdateCategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
