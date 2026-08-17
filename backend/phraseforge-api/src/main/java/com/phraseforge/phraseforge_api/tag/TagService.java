package com.phraseforge.phraseforge_api.tag;

import com.phraseforge.phraseforge_api.common.PagedResponse;
import com.phraseforge.phraseforge_api.exception.DuplicateResourceException;
import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
import com.phraseforge.phraseforge_api.phrase.PhraseTagRepository;
import com.phraseforge.phraseforge_api.tag.dto.CreateTagRequest;
import com.phraseforge.phraseforge_api.tag.dto.TagResponse;
import com.phraseforge.phraseforge_api.tag.dto.UpdateTagRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;
    private final PhraseTagRepository phraseTagRepository;

    public TagService(TagRepository tagRepository, TagMapper tagMapper,
                      PhraseTagRepository phraseTagRepository) {
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
        this.phraseTagRepository = phraseTagRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<TagResponse> list(Pageable pageable) {
        Page<Tag> page = tagRepository.findAll(pageable);
        return PagedResponse.from(page.map(tagMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public TagResponse getById(Long id) {
        return tagMapper.toResponse(findByIdOrThrow(id));
    }

    @Transactional
    public TagResponse create(CreateTagRequest request) {
        ensureNameAvailable(request.name());
        Tag tag = tagRepository.save(new Tag(request.name()));
        return tagMapper.toResponse(tag);
    }

    @Transactional
    public TagResponse update(Long id, UpdateTagRequest request) {
        Tag tag = findByIdOrThrow(id);
        if (!tag.getName().equals(request.name()) && tagRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Tag already exists: " + request.name());
        }
        tag.setName(request.name());
        return tagMapper.toResponse(tag);
    }

    @Transactional
    public void delete(Long id) {
        findByIdOrThrow(id);
        phraseTagRepository.deleteByTagId(id);
        tagRepository.deleteById(id);
    }

    private Tag findByIdOrThrow(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + id));
    }

    private void ensureNameAvailable(String name) {
        if (tagRepository.existsByName(name)) {
            throw new DuplicateResourceException("Tag already exists: " + name);
        }
    }
}
