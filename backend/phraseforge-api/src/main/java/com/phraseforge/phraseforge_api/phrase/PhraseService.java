package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.author.Author;
import com.phraseforge.phraseforge_api.author.AuthorRepository;
import com.phraseforge.phraseforge_api.category.Category;
import com.phraseforge.phraseforge_api.category.CategoryRepository;
import com.phraseforge.phraseforge_api.common.PagedResponse;
import com.phraseforge.phraseforge_api.exception.DuplicateResourceException;
import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
import com.phraseforge.phraseforge_api.favorite.FavoriteService;
import com.phraseforge.phraseforge_api.phrase.dto.CreatePhraseRequest;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseResponse;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseSummaryResponse;
import com.phraseforge.phraseforge_api.phrase.dto.UpdatePhraseRequest;
import com.phraseforge.phraseforge_api.tag.Tag;
import com.phraseforge.phraseforge_api.tag.TagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.security.SecureRandom;
import java.util.Set;

@Service
public class PhraseService {

    private final PhraseRepository phraseRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PhraseMapper phraseMapper;
    private final FavoriteService favoriteService;
    private final SecureRandom secureRandom = new SecureRandom();

    public PhraseService(PhraseRepository phraseRepository,
                         AuthorRepository authorRepository,
                         CategoryRepository categoryRepository,
                         TagRepository tagRepository,
                         PhraseMapper phraseMapper,
                         FavoriteService favoriteService) {
        this.phraseRepository = phraseRepository;
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.phraseMapper = phraseMapper;
        this.favoriteService = favoriteService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<PhraseSummaryResponse> list(String query, Long authorId, Long categoryId,
                                                     Long tagId, String language, Pageable pageable) {
        Specification<Phrase> spec = PhraseSpecifications.filter(query, authorId, categoryId, tagId, language);
        Page<Phrase> page = phraseRepository.findAll(spec, pageable);
        Set<Long> favoritedIds = favoriteService.favoritedPhraseIds(currentUserId(), page.stream()
                .map(Phrase::getId)
                .collect(java.util.stream.Collectors.toSet()));
        return PagedResponse.from(page.map(phrase -> phraseMapper.toSummary(phrase, favoritedIds.contains(phrase.getId()))));
    }

    @Transactional(readOnly = true)
    public PhraseResponse getById(Long id) {
        Phrase phrase = findWithDetailsOrThrow(id);
        boolean favorited = favoriteService.favoritedPhraseIds(currentUserId(), Set.of(phrase.getId()))
                .contains(phrase.getId());
        return phraseMapper.toResponse(phrase, favorited);
    }

    @Transactional
    public PhraseResponse create(CreatePhraseRequest request) {
        Author author = authorRepository.findById(request.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("Author not found: " + request.authorId()));
        ensureNotDuplicate(request.content(), request.authorId());

        Phrase phrase = new Phrase(request.content(), author, request.year(), request.language(), request.source());
        attachCategories(phrase, request.categoryIds());
        attachTags(phrase, request.tagIds());
        return phraseMapper.toResponse(phraseRepository.save(phrase));
    }

    @Transactional
    public PhraseResponse update(Long id, UpdatePhraseRequest request) {
        Phrase phrase = findWithDetailsOrThrow(id);
        ensureNotDuplicateExcluding(request.content(), request.authorId(), id);

        Author author = authorRepository.findById(request.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("Author not found: " + request.authorId()));
        phrase.setContent(request.content());
        phrase.setAuthor(author);
        phrase.setYear(request.year());
        phrase.setLanguage(request.language());
        phrase.setSource(request.source());
        replaceCategories(phrase, request.categoryIds());
        replaceTags(phrase, request.tagIds());
        return phraseMapper.toResponse(phrase);
    }

    @Transactional
    public void delete(Long id) {
        Phrase phrase = findWithDetailsOrThrow(id);
        phraseRepository.delete(phrase);
    }

    @Transactional(readOnly = true)
    public PhraseResponse random() {
        long total = phraseRepository.count();
        if (total == 0) {
            throw new ResourceNotFoundException("No phrases available");
        }
        int randomIndex = secureRandom.nextInt((int) total);
        Page<Phrase> page = phraseRepository.findAll(PageRequest.of(randomIndex, 1));
        Phrase phrase = page.getContent().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No phrases available"));
        boolean favorited = favoriteService.favoritedPhraseIds(currentUserId(), Set.of(phrase.getId()))
                .contains(phrase.getId());
        return phraseMapper.toResponse(phrase, favorited);
    }

    private void ensureNotDuplicate(String content, Long authorId) {
        if (phraseRepository.existsByContentAndAuthor_Id(content, authorId)) {
            throw new DuplicateResourceException(
                    "Phrase with the same content already exists for this author");
        }
    }

    private void ensureNotDuplicateExcluding(String content, Long authorId, Long excludeId) {
        // Runs at the database level; excludes the phrase being updated so an
        // unchanged phrase does not trip its own duplicate check.
        if (phraseRepository.existsByContentAndAuthor_IdAndIdNot(content, authorId, excludeId)) {
            throw new DuplicateResourceException(
                    "Phrase with the same content already exists for this author");
        }
    }

    private Phrase findWithDetailsOrThrow(Long id) {
        return phraseRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phrase not found: " + id));
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void attachCategories(Phrase phrase, Set<Long> categoryIds) {
        categoryIds.forEach(id -> {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
            phrase.getPhraseCategories().add(new PhraseCategory(phrase, category));
        });
    }

    private void attachTags(Phrase phrase, Set<Long> tagIds) {
        tagIds.forEach(id -> {
            Tag tag = tagRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + id));
            phrase.getPhraseTags().add(new PhraseTag(phrase, tag));
        });
    }

    private void replaceCategories(Phrase phrase, Set<Long> categoryIds) {
        phrase.getPhraseCategories().clear();
        attachCategories(phrase, categoryIds);
    }

    private void replaceTags(Phrase phrase, Set<Long> tagIds) {
        phrase.getPhraseTags().clear();
        attachTags(phrase, tagIds);
    }
}
