package com.phraseforge.phraseforge_api.author;

import com.phraseforge.phraseforge_api.author.dto.AuthorResponse;
import com.phraseforge.phraseforge_api.author.dto.AuthorSummaryResponse;
import com.phraseforge.phraseforge_api.author.dto.CreateAuthorRequest;
import com.phraseforge.phraseforge_api.author.dto.UpdateAuthorRequest;
import com.phraseforge.phraseforge_api.common.PagedResponse;
import com.phraseforge.phraseforge_api.common.SlugUtil;
import com.phraseforge.phraseforge_api.exception.DuplicateResourceException;
import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final AuthorMapper authorMapper;

    public AuthorService(AuthorRepository authorRepository, AuthorMapper authorMapper) {
        this.authorRepository = authorRepository;
        this.authorMapper = authorMapper;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuthorSummaryResponse> list(Pageable pageable) {
        Page<Author> page = authorRepository.findAll(pageable);
        Map<Long, Long> counts = phraseCounts();
        return PagedResponse.from(page.map(author ->
                authorMapper.toSummary(author, counts.getOrDefault(author.getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public AuthorResponse getById(Long id) {
        Author author = findByIdOrThrow(id);
        return authorMapper.toResponse(author, phraseCountFor(id));
    }

    @Transactional(readOnly = true)
    public void ensureExists(Long id) {
        findByIdOrThrow(id);
    }

    @Transactional
    public AuthorResponse create(CreateAuthorRequest request) {
        ensureNameAvailable(request.name());
        Author author = new Author(
                request.name(),
                SlugUtil.toSlug(request.name()),
                request.birthYear(),
                request.deathYear(),
                request.biography());
        ensureSlugAvailable(author.getSlug());
        Author saved = authorRepository.save(author);
        return authorMapper.toResponse(saved, 0L);
    }

    @Transactional
    public AuthorResponse update(Long id, UpdateAuthorRequest request) {
        Author author = findByIdOrThrow(id);
        ensureNameAvailableForUpdate(request.name(), author);
        author.setName(request.name());
        author.setSlug(SlugUtil.toSlug(request.name()));
        author.setBirthYear(request.birthYear());
        author.setDeathYear(request.deathYear());
        author.setBiography(request.biography());
        return authorMapper.toResponse(author, phraseCountFor(id));
    }

    @Transactional
    public void delete(Long id) {
        Author author = findByIdOrThrow(id);
        if (!author.getPhrases().isEmpty()) {
            throw new DuplicateResourceException(
                    "Author with id " + id + " has phrases and cannot be deleted");
        }
        authorRepository.delete(author);
    }

    private Author findByIdOrThrow(Long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found: " + id));
    }

    private void ensureNameAvailable(String name) {
        if (authorRepository.existsByName(name)) {
            throw new DuplicateResourceException("Author already exists: " + name);
        }
    }

    private void ensureNameAvailableForUpdate(String name, Author current) {
        if (!current.getName().equals(name) && authorRepository.existsByName(name)) {
            throw new DuplicateResourceException("Author already exists: " + name);
        }
    }

    private void ensureSlugAvailable(String slug) {
        if (authorRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Author slug already exists: " + slug);
        }
    }

    private Map<Long, Long> phraseCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : authorRepository.findPhraseCounts()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private long phraseCountFor(Long authorId) {
        return authorRepository.findPhraseCounts().stream()
                .filter(row -> ((Long) row[0]).equals(authorId))
                .map(row -> (Long) row[1])
                .findFirst()
                .orElse(0L);
    }
}
