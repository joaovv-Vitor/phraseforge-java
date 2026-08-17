package com.phraseforge.phraseforge_api.author;

import com.phraseforge.phraseforge_api.author.dto.CreateAuthorRequest;
import com.phraseforge.phraseforge_api.author.dto.UpdateAuthorRequest;
import com.phraseforge.phraseforge_api.common.SlugUtil;
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
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @Spy
    private AuthorMapper authorMapper;

    @InjectMocks
    private AuthorService authorService;

    @Test
    void create_generatesSlugAndSaves() {
        when(authorRepository.existsByName("Test Author")).thenReturn(false);
        when(authorRepository.existsBySlug("test-author")).thenReturn(false);
        when(authorRepository.save(any(Author.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var request = new CreateAuthorRequest("Test Author", 1900, 1980, "Test biography");
        var response = authorService.create(request);

        assertThat(response.slug()).isEqualTo("test-author");
        assertThat(response.name()).isEqualTo("Test Author");
        verify(authorRepository).save(any(Author.class));
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(authorRepository.existsByName("Test Author")).thenReturn(true);

        var request = new CreateAuthorRequest("Test Author", null, null, null);

        assertThatThrownBy(() -> authorService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(authorRepository, never()).save(any(Author.class));
    }

    @Test
    void update_missingAuthor_throwsNotFound() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new UpdateAuthorRequest("New", null, null, null);

        assertThatThrownBy(() -> authorService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void ensureExists_missingAuthor_throwsNotFound() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.ensureExists(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
