package com.phraseforge.phraseforge_api.tag;

import com.phraseforge.phraseforge_api.tag.dto.CreateTagRequest;
import com.phraseforge.phraseforge_api.tag.dto.TagResponse;
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

@WebMvcTest(TagController.class)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TagService tagService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void getById_returnsTag() throws Exception {
        when(tagService.getById(1L)).thenReturn(new TagResponse(1L, "mind"));

        mockMvc.perform(get("/api/v1/tags/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("mind")));
    }

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        when(tagService.create(any(CreateTagRequest.class))).thenReturn(new TagResponse(1L, "mind"));

        String body = objectMapper.writeValueAsString(new CreateTagRequest("mind"));

        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("mind")));
    }
}
