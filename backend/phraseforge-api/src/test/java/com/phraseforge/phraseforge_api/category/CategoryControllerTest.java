package com.phraseforge.phraseforge_api.category;

import com.phraseforge.phraseforge_api.category.dto.CategoryResponse;
import com.phraseforge.phraseforge_api.category.dto.CreateCategoryRequest;
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

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void getById_returnsCategory() throws Exception {
        when(categoryService.getById(1L)).thenReturn(new CategoryResponse(1L, "Stoicism", "stoicism", null, 0L));

        mockMvc.perform(get("/api/v1/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Stoicism")));
    }

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        when(categoryService.create(any(CreateCategoryRequest.class)))
                .thenReturn(new CategoryResponse(1L, "Stoicism", "stoicism", null, 0L));

        String body = objectMapper.writeValueAsString(new CreateCategoryRequest("Stoicism", null));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug", is("stoicism")));
    }
}
