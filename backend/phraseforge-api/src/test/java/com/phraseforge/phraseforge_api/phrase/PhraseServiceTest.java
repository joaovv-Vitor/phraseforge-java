package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.author.Author;
import com.phraseforge.phraseforge_api.author.AuthorRepository;
import com.phraseforge.phraseforge_api.exception.DuplicateResourceException;
import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
import com.phraseforge.phraseforge_api.phrase.dto.CreatePhraseRequest;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseResponse;
import com.phraseforge.phraseforge_api.phrase.dto.UpdatePhraseRequest;
import com.phraseforge.phraseforge_api.tag.Tag;
import com.phraseforge.phraseforge_api.tag.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhraseServiceTest {

    @Mock
    private PhraseRepository phraseRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private com.phraseforge.phraseforge_api.category.CategoryRepository categoryRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private PhraseMapper phraseMapper;

    @InjectMocks
    private PhraseService phraseService;

    @Test
    void create_savesPhrase() {
        Author author = new Author("Test Author", "test-author", null, null, null);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(phraseRepository.existsByContentAndAuthor_Id("Shared content", 1L)).thenReturn(false);
        when(phraseRepository.save(any(Phrase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(phraseMapper.toResponse(any(Phrase.class)))
                .thenReturn(new PhraseResponse(1L, "Shared content", null, "en", null, null, null, null, null, null));

        PhraseResponse response = phraseService.create(
                new CreatePhraseRequest("Shared content", 1L, null, "en", null, Set.of(), Set.of()));

        assertThat(response.content()).isEqualTo("Shared content");
        verify(phraseRepository).save(any(Phrase.class));
    }

    @Test
    void create_duplicateContentAndAuthor_throwsConflict() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(new Author("Test Author", "test-author", null, null, null)));
        when(phraseRepository.existsByContentAndAuthor_Id("Shared content", 1L)).thenReturn(true);

        assertThatThrownBy(() -> phraseService.create(
                new CreatePhraseRequest("Shared content", 1L, null, "en", null, Set.of(), Set.of())))
                .isInstanceOf(DuplicateResourceException.class);
        verify(phraseRepository, never()).save(any(Phrase.class));
    }

    @Test
    void update_duplicateContentAndAuthor_throwsConflict() {
        Phrase existing = new Phrase("Original content", new Author("Test Author", "test-author", null, null, null), null, "en", null);
        when(phraseRepository.findWithDetailsById(99L)).thenReturn(Optional.of(existing));
        when(phraseRepository.existsByContentAndAuthor_IdAndIdNot("Shared content", 1L, 99L)).thenReturn(true);

        assertThatThrownBy(() -> phraseService.update(99L,
                new UpdatePhraseRequest("Shared content", 1L, null, "en", null, Set.of(), Set.of())))
                .isInstanceOf(DuplicateResourceException.class);
        verify(phraseRepository, never()).save(any(Phrase.class));
    }

    @Test
    void update_missingPhrase_throwsNotFound() {
        when(phraseRepository.findWithDetailsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> phraseService.update(99L,
                new UpdatePhraseRequest("x", 1L, null, "en", null, Set.of(), Set.of())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void random_emptyDatabase_throwsNotFound() {
        when(phraseRepository.count()).thenReturn(0L);

        assertThatThrownBy(() -> phraseService.random())
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
