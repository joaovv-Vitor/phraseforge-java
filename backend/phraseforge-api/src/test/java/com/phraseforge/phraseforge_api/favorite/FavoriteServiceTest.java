package com.phraseforge.phraseforge_api.favorite;

import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
import com.phraseforge.phraseforge_api.phrase.Phrase;
import com.phraseforge.phraseforge_api.phrase.PhraseMapper;
import com.phraseforge.phraseforge_api.phrase.PhraseRepository;
import com.phraseforge.phraseforge_api.user.User;
import com.phraseforge.phraseforge_api.user.UserRepository;
import com.phraseforge.phraseforge_api.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PhraseRepository phraseRepository;
    @Mock
    private PhraseMapper phraseMapper;

    @InjectMocks
    private FavoriteService favoriteService;

    @Test
    void add_createsFavoriteWhenItDoesNotExist() {
        User user = new User("user@example.com", "{bcrypt}hash", "User", UserRole.USER);
        Phrase phrase = new Phrase("Know thyself.", null, null, "en", null);
        when(favoriteRepository.existsByUser_IdAndPhrase_Id(1L, 2L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(phraseRepository.findById(2L)).thenReturn(Optional.of(phrase));

        favoriteService.add(1L, 2L);

        ArgumentCaptor<Favorite> favorite = ArgumentCaptor.forClass(Favorite.class);
        verify(favoriteRepository).save(favorite.capture());
        assertThat(favorite.getValue().getPhrase()).isSameAs(phrase);
    }

    @Test
    void add_isIdempotentWhenFavoriteExists() {
        when(favoriteRepository.existsByUser_IdAndPhrase_Id(1L, 2L)).thenReturn(true);

        favoriteService.add(1L, 2L);

        verify(favoriteRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void add_missingPhrase_throwsNotFound() {
        User user = new User("user@example.com", "{bcrypt}hash", "User", UserRole.USER);
        when(favoriteRepository.existsByUser_IdAndPhrase_Id(1L, 2L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(phraseRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.add(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Phrase not found: 2");
    }

    @Test
    void remove_isIdempotent() {
        favoriteService.remove(1L, 2L);

        verify(favoriteRepository).deleteByUser_IdAndPhrase_Id(1L, 2L);
    }
}
