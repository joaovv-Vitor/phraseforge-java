package com.phraseforge.phraseforge_api.tag;

import com.phraseforge.phraseforge_api.exception.DuplicateResourceException;
import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
import com.phraseforge.phraseforge_api.tag.dto.CreateTagRequest;
import com.phraseforge.phraseforge_api.tag.dto.UpdateTagRequest;
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
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private com.phraseforge.phraseforge_api.phrase.PhraseTagRepository phraseTagRepository;

    @Spy
    private TagMapper tagMapper;

    @InjectMocks
    private TagService tagService;

    @Test
    void create_savesTag() {
        when(tagRepository.existsByName("mind")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = tagService.create(new CreateTagRequest("mind"));

        assertThat(response.name()).isEqualTo("mind");
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(tagRepository.existsByName("mind")).thenReturn(true);

        assertThatThrownBy(() -> tagService.create(new CreateTagRequest("mind")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void update_missingTag_throwsNotFound() {
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.update(99L, new UpdateTagRequest("x")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
