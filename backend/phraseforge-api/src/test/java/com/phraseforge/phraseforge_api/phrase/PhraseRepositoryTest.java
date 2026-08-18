package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.author.Author;
import com.phraseforge.phraseforge_api.author.AuthorRepository;
import com.phraseforge.phraseforge_api.support.DatabaseFixtureCleanup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PhraseRepositoryTest extends DatabaseFixtureCleanup {

    @Autowired
    private PhraseRepository phraseRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Test
    void existsByContentAndAuthorId_detectsDuplicates() {
        Author author = authorRepository.save(new Author("Test Author", "test-author", null, null, null));
        phraseRepository.save(new Phrase("Shared content", author, null, "en", null));

        boolean exists = phraseRepository.existsByContentAndAuthor_Id("Shared content", author.getId());
        boolean otherAuthor = phraseRepository.existsByContentAndAuthor_Id("Shared content", 999L);

        assertThat(exists).isTrue();
        assertThat(otherAuthor).isFalse();
    }

    @Test
    void existsByContentAndAuthorIdAndIdNot_excludesGivenPhrase() {
        Author author = authorRepository.save(new Author("Test Author", "test-author", null, null, null));
        Phrase first = phraseRepository.save(new Phrase("Shared content", author, null, "en", null));
        Phrase second = phraseRepository.save(new Phrase("Shared content", author, null, "en", null));

        boolean excludingFirst = phraseRepository.existsByContentAndAuthor_IdAndIdNot("Shared content", author.getId(), first.getId());
        boolean excludingAll = phraseRepository.existsByContentAndAuthor_IdAndIdNot("Shared content", author.getId(), second.getId());

        assertThat(excludingFirst).isTrue();   // second still matches
        assertThat(excludingAll).isTrue();     // first still matches
    }

    @Test
    void existsByContentAndAuthorIdAndIdNot_returnsFalseWhenNoOtherMatch() {
        Author author = authorRepository.save(new Author("Test Author", "test-author", null, null, null));
        Phrase only = phraseRepository.save(new Phrase("Unique content", author, null, "en", null));

        boolean found = phraseRepository.existsByContentAndAuthor_IdAndIdNot("Unique content", author.getId(), only.getId());

        assertThat(found).isFalse();
    }

    @Test
    void count_returnsTotal() {
        Author author = authorRepository.save(new Author("Test Author", "test-author", null, null, null));
        phraseRepository.save(new Phrase("One", author, null, "en", null));
        phraseRepository.save(new Phrase("Two", author, null, "pt", null));

        assertThat(phraseRepository.count()).isEqualTo(2L);
    }
}
