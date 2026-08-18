package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.author.Author;
import com.phraseforge.phraseforge_api.author.AuthorRepository;
import com.phraseforge.phraseforge_api.category.Category;
import com.phraseforge.phraseforge_api.category.CategoryRepository;
import com.phraseforge.phraseforge_api.support.DatabaseFixtureCleanup;
import com.phraseforge.phraseforge_api.tag.Tag;
import com.phraseforge.phraseforge_api.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PhraseSpecificationsTest extends DatabaseFixtureCleanup {

    @Autowired
    private PhraseRepository phraseRepository;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TagRepository tagRepository;

    private Author authorA;
    private Author authorB;
    private Category philosophy;
    private Category stoicism;
    private Tag mind;
    private Tag strength;

    @BeforeEach
    void setUp() {
        authorA = authorRepository.save(new Author("Test Author A", "test-author-a", null, null, null));
        authorB = authorRepository.save(new Author("Test Author B", "test-author-b", null, null, null));
        philosophy = categoryRepository.save(new Category("Philosophy", "philosophy", null));
        stoicism = categoryRepository.save(new Category("Stoicism", "stoicism", null));
        mind = tagRepository.save(new Tag("mind"));
        strength = tagRepository.save(new Tag("strength"));

        // Phrase 1: authorA, philosophy, mind
        savePhrase("The unexamined life", authorA, "en", List.of(philosophy), List.of(mind));
        // Phrase 2: authorA, philosophy AND stoicism, mind AND strength (multi-join: dedup must hold)
        savePhrase("What stands in the way becomes the way", authorA, "en", List.of(philosophy, stoicism), List.of(mind, strength));
        // Phrase 3: authorB, stoicism, strength
        savePhrase("Apenas os instruídos são livres", authorB, "pt", List.of(stoicism), List.of(strength));
    }

    private void savePhrase(String content, Author author, String language,
                            List<Category> categories, List<Tag> tags) {
        Phrase phrase = new Phrase(content, author, null, language, null);
        categories.forEach(c -> phrase.getPhraseCategories().add(new PhraseCategory(phrase, c)));
        tags.forEach(t -> phrase.getPhraseTags().add(new PhraseTag(phrase, t)));
        phraseRepository.save(phrase);
    }

    private List<Long> findIds(Specification<Phrase> spec) {
        Page<Phrase> page = phraseRepository.findAll(spec, PageRequest.of(0, 20));
        return page.getContent().stream().map(Phrase::getId).toList();
    }

    @Test
    void query_matchesContent() {
        List<Long> ids = findIds(PhraseSpecifications.filter("unexamined", null, null, null, null));
        assertThat(ids).hasSize(1);
    }

    @Test
    void query_matchesAuthorName() {
        List<Long> ids = findIds(PhraseSpecifications.filter("Author B", null, null, null, null));
        assertThat(ids).hasSize(1);
    }

    @Test
    void authorId_filtersByAuthor() {
        List<Long> ids = findIds(PhraseSpecifications.filter(null, authorB.getId(), null, null, null));
        assertThat(ids).hasSize(1);
    }

    @Test
    void categoryId_filtersByCategory() {
        List<Long> ids = findIds(PhraseSpecifications.filter(null, null, philosophy.getId(), null, null));
        assertThat(ids).hasSize(2);
    }

    @Test
    void tagId_filtersByTag() {
        List<Long> ids = findIds(PhraseSpecifications.filter(null, null, null, mind.getId(), null));
        assertThat(ids).hasSize(2);
    }

    @Test
    void language_filtersByLanguage() {
        List<Long> ids = findIds(PhraseSpecifications.filter(null, null, null, null, "pt"));
        assertThat(ids).hasSize(1);
    }

    @Test
    void combinations_authorAndCategoryAndTag() {
        List<Long> ids = findIds(PhraseSpecifications.filter(null, authorA.getId(), stoicism.getId(), mind.getId(), "en"));
        assertThat(ids).hasSize(1);
    }

    @Test
    void categoryFilter_doesNotDuplicatePhrases() {
        // Phrase 2 belongs to BOTH philosophy and stoicism and has two tags.
        // A cartesian join would return it twice; distinct() must collapse it.
        List<Long> ids = findIds(PhraseSpecifications.filter(null, null, stoicism.getId(), null, null));
        assertThat(ids).hasSize(2);
        assertThat(ids).doesNotHaveDuplicates();
    }

    @Test
    void tagFilter_doesNotDuplicatePhrases() {
        List<Long> ids = findIds(PhraseSpecifications.filter(null, null, null, strength.getId(), null));
        assertThat(ids).hasSize(2);
        assertThat(ids).doesNotHaveDuplicates();
    }

    @Test
    void paginatedListing_preservesPageBoundariesAndLoadsCollections() {
        Specification<Phrase> spec = PhraseSpecifications.filter(null, null, null, null, null);
        PageRequest pageRequest = PageRequest.of(0, 1, Sort.by("id"));

        Page<Phrase> firstPage = phraseRepository.findAll(spec, pageRequest);
        Page<Phrase> secondPage = phraseRepository.findAll(spec, pageRequest.next());
        Page<Phrase> thirdPage = phraseRepository.findAll(spec, pageRequest.next().next());

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);

        List<Long> ids = List.of(firstPage, secondPage, thirdPage).stream()
                .flatMap(page -> page.getContent().stream())
                .map(Phrase::getId)
                .toList();
        assertThat(ids).hasSize(3).doesNotHaveDuplicates();

        Phrase secondPhrase = secondPage.getContent().getFirst();
        assertThat(secondPhrase.getAuthor()).isNotNull();
        assertThat(secondPhrase.getCategories()).hasSize(2);
        assertThat(secondPhrase.getTags()).hasSize(2);
    }
}
