package com.phraseforge.phraseforge_api.favorite;

import com.phraseforge.phraseforge_api.common.PagedResponse;
import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
import com.phraseforge.phraseforge_api.phrase.Phrase;
import com.phraseforge.phraseforge_api.phrase.PhraseMapper;
import com.phraseforge.phraseforge_api.phrase.PhraseRepository;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseSummaryResponse;
import com.phraseforge.phraseforge_api.user.User;
import com.phraseforge.phraseforge_api.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final PhraseRepository phraseRepository;
    private final PhraseMapper phraseMapper;

    public FavoriteService(
            FavoriteRepository favoriteRepository,
            UserRepository userRepository,
            PhraseRepository phraseRepository,
            PhraseMapper phraseMapper) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.phraseRepository = phraseRepository;
        this.phraseMapper = phraseMapper;
    }

    @Transactional(readOnly = true)
    public PagedResponse<PhraseSummaryResponse> list(Long userId, Pageable pageable) {
        return PagedResponse.from(favoriteRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable)
                .map(favorite -> phraseMapper.toSummary(favorite.getPhrase(), true)));
    }

    @Transactional
    public void add(Long userId, Long phraseId) {
        if (favoriteRepository.existsByUser_IdAndPhrase_Id(userId, phraseId)) {
            return;
        }
        User user = findUser(userId);
        Phrase phrase = phraseRepository.findById(phraseId)
                .orElseThrow(() -> new ResourceNotFoundException("Phrase not found: " + phraseId));
        favoriteRepository.save(new Favorite(user, phrase, Instant.now()));
    }

    @Transactional
    public void remove(Long userId, Long phraseId) {
        favoriteRepository.deleteByUser_IdAndPhrase_Id(userId, phraseId);
    }

    @Transactional(readOnly = true)
    public Set<Long> favoritedPhraseIds(Long userId, Set<Long> phraseIds) {
        if (userId == null || phraseIds.isEmpty()) {
            return Set.of();
        }
        return favoriteRepository.findFavoritedPhraseIds(userId, phraseIds);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
