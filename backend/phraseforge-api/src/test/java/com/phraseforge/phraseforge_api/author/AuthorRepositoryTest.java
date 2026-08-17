package com.phraseforge.phraseforge_api.author;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuthorRepositoryTest {

    @Autowired
    private AuthorRepository authorRepository;

    @Test
    void findBySlug_returnsAuthor() {
        authorRepository.save(new Author("Test Author", "test-author", 1900, 1980, null));

        Optional<Author> found = authorRepository.findBySlug("test-author");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Author");
    }

    @Test
    void findPhraseCounts_returnsZeroWhenNoPhrases() {
        authorRepository.save(new Author("Test Author", "test-author", null, null, null));

        List<Object[]> counts = authorRepository.findPhraseCounts();

        assertThat(counts).hasSize(1);
        assertThat((Long) counts.get(0)[0]).isNotNull();
        assertThat((Long) counts.get(0)[1]).isZero();
    }
}
