package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.phrase.dto.CreatePhraseRequest;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PhraseController.class)
class PhraseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PhraseService phraseService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void getById_returnsPhrase() throws Exception {
        when(phraseService.getById(1L)).thenReturn(
                new PhraseResponse(1L, "Know thyself.", null, "en", null, null, null, null, null, null));

        mockMvc.perform(get("/api/v1/phrases/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", is("Know thyself.")));
    }

    @Test
    void random_returnsPhrase() throws Exception {
        when(phraseService.random()).thenReturn(
                new PhraseResponse(2L, "Be good.", null, "en", null, null, null, null, null, null));

        mockMvc.perform(get("/api/v1/phrases/random"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(2)));
    }

    @Test
    void create_invalidRequest_returnsBadRequest() throws Exception {
        // Empty content and missing authorId violate Bean Validation.
        mockMvc.perform(post("/api/v1/phrases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\",\"authorId\":null,\"language\":\"en\"}"))
                .andExpect(status().isBadRequest());
    }
}
