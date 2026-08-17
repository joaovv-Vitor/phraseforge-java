package com.phraseforge.phraseforge_api.phrase;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PhraseSpecifications {

    private PhraseSpecifications() {
    }

    public static Specification<Phrase> filter(String query, Long authorId,
                                               Long categoryId, Long tagId, String language) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                Join<Phrase, ?> authorJoin = root.join("author", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("content")), like),
                        cb.like(cb.lower(authorJoin.get("name")), like)));
            }

            if (authorId != null) {
                predicates.add(cb.equal(root.get("author").get("id"), authorId));
            }

            if (categoryId != null) {
                root.join("phraseCategories", JoinType.LEFT).get("category");
                cq.distinct(true);
                predicates.add(cb.equal(root.get("phraseCategories").get("category").get("id"), categoryId));
            }

            if (tagId != null) {
                predicates.add(cb.equal(root.get("phraseTags").get("tag").get("id"), tagId));
            }

            if (language != null && !language.isBlank()) {
                predicates.add(cb.equal(root.get("language"), language));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
