package com.phraseforge.phraseforge_api.category;

import com.phraseforge.phraseforge_api.category.dto.CategoryResponse;
import com.phraseforge.phraseforge_api.category.dto.CategorySummaryResponse;
import com.phraseforge.phraseforge_api.category.dto.CreateCategoryRequest;
import com.phraseforge.phraseforge_api.category.dto.UpdateCategoryRequest;
import com.phraseforge.phraseforge_api.common.PagedResponse;
import com.phraseforge.phraseforge_api.common.SlugUtil;
import com.phraseforge.phraseforge_api.exception.DuplicateResourceException;
import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
import com.phraseforge.phraseforge_api.phrase.PhraseCategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final PhraseCategoryRepository phraseCategoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository,
                           PhraseCategoryRepository phraseCategoryRepository,
                           CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.phraseCategoryRepository = phraseCategoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Transactional(readOnly = true)
    public PagedResponse<CategorySummaryResponse> list(Pageable pageable) {
        Page<Category> page = categoryRepository.findAll(pageable);
        Map<Long, Long> counts = phraseCounts();
        return PagedResponse.from(page.map(category ->
                categoryMapper.toSummary(category, counts.getOrDefault(category.getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category category = findByIdOrThrow(id);
        return categoryMapper.toResponse(category, phraseCountFor(id));
    }

    @Transactional(readOnly = true)
    public void ensureExists(Long id) {
        findByIdOrThrow(id);
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        ensureNameAvailable(request.name());
        Category category = new Category(
                request.name(),
                SlugUtil.toSlug(request.name()),
                request.description());
        ensureSlugAvailable(category.getSlug());
        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved, 0L);
    }

    @Transactional
    public CategoryResponse update(Long id, UpdateCategoryRequest request) {
        Category category = findByIdOrThrow(id);
        if (!category.getName().equals(request.name()) && categoryRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Category already exists: " + request.name());
        }
        category.setName(request.name());
        category.setSlug(SlugUtil.toSlug(request.name()));
        category.setDescription(request.description());
        return categoryMapper.toResponse(category, phraseCountFor(id));
    }

    @Transactional
    public void delete(Long id) {
        Category category = findByIdOrThrow(id);
        phraseCategoryRepository.deleteByCategoryId(id);
        categoryRepository.delete(category);
    }

    private Category findByIdOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    private void ensureNameAvailable(String name) {
        if (categoryRepository.existsByName(name)) {
            throw new DuplicateResourceException("Category already exists: " + name);
        }
    }

    private void ensureSlugAvailable(String slug) {
        if (categoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Category slug already exists: " + slug);
        }
    }

    private Map<Long, Long> phraseCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : phraseCategoryRepository.findPhraseCounts()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private long phraseCountFor(Long categoryId) {
        return phraseCategoryRepository.findPhraseCounts().stream()
                .filter(row -> ((Long) row[0]).equals(categoryId))
                .map(row -> (Long) row[1])
                .findFirst()
                .orElse(0L);
    }
}
