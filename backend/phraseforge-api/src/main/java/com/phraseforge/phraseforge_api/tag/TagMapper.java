package com.phraseforge.phraseforge_api.tag;

import com.phraseforge.phraseforge_api.tag.dto.TagResponse;
import org.springframework.stereotype.Component;

@Component
public class TagMapper {

    public TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
