package com.phraseforge.phraseforge_api.category;

import com.phraseforge.phraseforge_api.category.dto.CreateCategoryRequest;
import com.phraseforge.phraseforge_api.exception.DuplicateResourceException;
import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private com.phraseforge.phraseforge_api.phrase.PhraseCategoryRepository phraseCategoryRepository;

    @Spy
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void create_generatesSlugAndSaves() {
        when(categoryRepository.existsByName("Stoicism")).thenReturn(false);
        when(categoryRepository.existsBySlug("stoicism")).thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = categoryService.create(new CreateCategoryRequest("Stoicism", "Ancient school"));

        assertThat(response.slug()).isEqualTo("stoicism");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(categoryRepository.existsByName("Stoicism")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CreateCategoryRequest("Stoicism", null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void delete_missingCategory_throwsNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
