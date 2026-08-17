package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.author.Author;
import com.phraseforge.phraseforge_api.author.AuthorRepository;
import com.phraseforge.phraseforge_api.category.Category;
import com.phraseforge.phraseforge_api.category.CategoryRepository;
import com.phraseforge.phraseforge_api.common.PagedResponse;
import com.phraseforge.phraseforge_api.exception.DuplicateResourceException;
import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
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

import java.security.SecureRandom;
import java.util.Set;

@Service
public class PhraseService {

    private final PhraseRepository phraseRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PhraseMapper phraseMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public PhraseService(PhraseRepository phraseRepository,
                         AuthorRepository authorRepository,
                         CategoryRepository categoryRepository,
                         TagRepository tagRepository,
                         PhraseMapper phraseMapper) {
        this.phraseRepository = phraseRepository;
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.phraseMapper = phraseMapper;
    }

    @Transactional(readOnly = true)
    public PagedResponse<PhraseSummaryResponse> list(String query, Long authorId, Long categoryId,
                                                     Long tagId, String language, Pageable pageable) {
        Specification<Phrase> spec = PhraseSpecifications.filter(query, authorId, categoryId, tagId, language);
        Page<Phrase> page = phraseRepository.findAll(spec, pageable);
        return PagedResponse.from(page.map(phraseMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public PhraseResponse getById(Long id) {
        return phraseMapper.toResponse(findWithDetailsOrThrow(id));
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
        return phraseMapper.toResponse(phrase);
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
