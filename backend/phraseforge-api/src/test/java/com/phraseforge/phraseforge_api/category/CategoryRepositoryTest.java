package com.phraseforge.phraseforge_api.category;

import com.phraseforge.phraseforge_api.support.DatabaseFixtureCleanup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CategoryRepositoryTest extends DatabaseFixtureCleanup {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void findBySlug_returnsCategory() {
        categoryRepository.save(new Category("Stoicism", "stoicism", "Ancient school"));

        Optional<Category> found = categoryRepository.findBySlug("stoicism");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Stoicism");
    }
}
