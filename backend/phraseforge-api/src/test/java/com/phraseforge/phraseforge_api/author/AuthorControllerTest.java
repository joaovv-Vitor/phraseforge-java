package com.phraseforge.phraseforge_api.author;

import com.phraseforge.phraseforge_api.author.dto.AuthorResponse;
import com.phraseforge.phraseforge_api.author.dto.CreateAuthorRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthorController.class)
class AuthorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthorService authorService;

    @MockitoBean
    private com.phraseforge.phraseforge_api.phrase.PhraseService phraseService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void getById_returnsAuthor() throws Exception {
        when(authorService.getById(1L)).thenReturn(
                new AuthorResponse(1L, "Test Author", "test-author", null, null, null, 0L, null, null));

        mockMvc.perform(get("/api/v1/authors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Test Author")))
                .andExpect(jsonPath("$.slug", is("test-author")));
    }

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        when(authorService.create(any(CreateAuthorRequest.class))).thenReturn(
                new AuthorResponse(1L, "Test Author", "test-author", null, null, null, 0L, null, null));

        String body = objectMapper.writeValueAsString(
                new CreateAuthorRequest("Test Author", null, null, null));

        mockMvc.perform(post("/api/v1/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Test Author")));
    }
}
