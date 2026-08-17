package com.phraseforge.phraseforge_api.phrase;

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
                var authorJoin = root.join("author", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("content")), like),
                        cb.like(cb.lower(authorJoin.get("name")), like)));
            }

            if (authorId != null) {
                predicates.add(cb.equal(root.join("author").get("id"), authorId));
            }

            boolean hasCollectionFilter = categoryId != null || tagId != null;
            if (hasCollectionFilter) {
                cq.distinct(true);
            }

            if (categoryId != null) {
                var catJoin = root.join("phraseCategories", JoinType.LEFT);
                predicates.add(cb.equal(catJoin.get("category").get("id"), categoryId));
            }

            if (tagId != null) {
                var tagJoin = root.join("phraseTags", JoinType.LEFT);
                predicates.add(cb.equal(tagJoin.get("tag").get("id"), tagId));
            }

            if (language != null && !language.isBlank()) {
                predicates.add(cb.equal(root.get("language"), language));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
