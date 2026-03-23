package com.crud.controller;

import com.crud.model.Category;
import com.crud.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService service;

    private Category createCategory(Long id, String name) {
        Category c = new Category(name, "Descrição");
        c.setId(id);
        return c;
    }

    @Nested
    @DisplayName("GET /categories")
    class ListTests {

        @Test
        @DisplayName("deve exibir lista de categorias")
        void shouldShowCategoryList() throws Exception {
            when(service.findAll()).thenReturn(List.of(
                    createCategory(1L, "Eletrônicos"),
                    createCategory(2L, "Roupas")
            ));

            mockMvc.perform(get("/categories"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("categories/list"))
                    .andExpect(model().attribute("categories", hasSize(2)));
        }

        @Test
        @DisplayName("deve exibir lista vazia")
        void shouldShowEmptyList() throws Exception {
            when(service.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/categories"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("categories", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /categories/new")
    class NewFormTests {

        @Test
        @DisplayName("deve exibir formulário de criação")
        void shouldShowCreateForm() throws Exception {
            mockMvc.perform(get("/categories/new"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("categories/form"))
                    .andExpect(model().attributeExists("category"));
        }
    }

    @Nested
    @DisplayName("GET /categories/edit/{id}")
    class EditFormTests {

        @Test
        @DisplayName("deve exibir formulário de edição")
        void shouldShowEditForm() throws Exception {
            when(service.findById(1L)).thenReturn(createCategory(1L, "Eletrônicos"));

            mockMvc.perform(get("/categories/edit/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("categories/form"))
                    .andExpect(model().attribute("category", hasProperty("name", is("Eletrônicos"))));
        }

        @Test
        @DisplayName("deve redirecionar quando categoria não existe")
        void shouldRedirectForNonExistentCategory() throws Exception {
            when(service.findById(999L)).thenThrow(new NoSuchElementException("Categoria não encontrada"));

            mockMvc.perform(get("/categories/edit/999"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/categories"))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Nested
    @DisplayName("POST /categories/save")
    class SaveTests {

        @Test
        @DisplayName("deve criar nova categoria com dados válidos")
        void shouldCreateCategory() throws Exception {
            when(service.save(any())).thenReturn(createCategory(1L, "Nova Categoria"));

            mockMvc.perform(post("/categories/save")
                            .param("name", "Nova Categoria")
                            .param("description", "Descrição"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/categories"));
        }

        @Test
        @DisplayName("deve retornar formulário com erros de validação")
        void shouldReturnFormOnValidationErrors() throws Exception {
            mockMvc.perform(post("/categories/save")
                            .param("name", ""))
                    .andExpect(status().isOk())
                    .andExpect(view().name("categories/form"))
                    .andExpect(model().hasErrors());
        }

        @Test
        @DisplayName("deve atualizar categoria existente")
        void shouldUpdateCategory() throws Exception {
            when(service.update(eq(1L), any())).thenReturn(createCategory(1L, "Atualizada"));

            mockMvc.perform(post("/categories/save")
                            .param("id", "1")
                            .param("name", "Atualizada")
                            .param("description", "Desc"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/categories"));
        }

        @Test
        @DisplayName("deve tratar exceção do service ao salvar")
        void shouldHandleServiceException() throws Exception {
            when(service.save(any())).thenThrow(new IllegalArgumentException("Erro de validação"));

            mockMvc.perform(post("/categories/save")
                            .param("name", "Categoria"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("categories/form"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @DisplayName("deve tratar exceção genérica ao salvar")
        void shouldHandleGenericException() throws Exception {
            when(service.save(any())).thenThrow(new RuntimeException("Erro inesperado"));

            mockMvc.perform(post("/categories/save")
                            .param("name", "Categoria"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("categories/form"))
                    .andExpect(model().attribute("error", "Erro inesperado ao salvar categoria. Tente novamente."));
        }
    }

    @Nested
    @DisplayName("POST /categories/delete/{id}")
    class DeleteTests {

        @Test
        @DisplayName("deve excluir categoria com sucesso")
        void shouldDeleteCategory() throws Exception {
            doNothing().when(service).delete(1L);

            mockMvc.perform(post("/categories/delete/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/categories"))
                    .andExpect(flash().attribute("success", "Categoria excluída com sucesso!"));
        }

        @Test
        @DisplayName("deve tratar exclusão de categoria inexistente")
        void shouldHandleDeleteNonExistent() throws Exception {
            doThrow(new NoSuchElementException("Categoria não encontrada"))
                    .when(service).delete(999L);

            mockMvc.perform(post("/categories/delete/999"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/categories"))
                    .andExpect(flash().attributeExists("error"));
        }

        @Test
        @DisplayName("deve tratar exclusão de categoria com produtos vinculados")
        void shouldHandleDeleteWithLinkedProducts() throws Exception {
            doThrow(new IllegalStateException("Não é possível excluir categoria com produtos vinculados"))
                    .when(service).delete(1L);

            mockMvc.perform(post("/categories/delete/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/categories"))
                    .andExpect(flash().attributeExists("error"));
        }

        @Test
        @DisplayName("deve tratar erro genérico ao excluir")
        void shouldHandleGenericDeleteError() throws Exception {
            doThrow(new RuntimeException("Erro inesperado"))
                    .when(service).delete(1L);

            mockMvc.perform(post("/categories/delete/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/categories"))
                    .andExpect(flash().attribute("error", "Erro inesperado ao excluir categoria."));
        }
    }
}
