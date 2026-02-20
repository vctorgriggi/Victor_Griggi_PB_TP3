package com.crud.controller;

import com.crud.model.Product;
import com.crud.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService service;

    private Product createProduct(Long id, String name) {
        Product p = new Product(name, "Descrição", new BigDecimal("99.99"), 10);
        p.setId(id);
        return p;
    }

    @Nested
    @DisplayName("GET /products")
    class ListTests {

        @Test
        @DisplayName("deve exibir lista de produtos")
        void shouldShowProductList() throws Exception {
            when(service.findAll()).thenReturn(List.of(
                    createProduct(1L, "Produto A"),
                    createProduct(2L, "Produto B")
            ));

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("products/list"))
                    .andExpect(model().attribute("products", hasSize(2)));
        }

        @Test
        @DisplayName("deve exibir lista vazia")
        void shouldShowEmptyList() throws Exception {
            when(service.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("products", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /products/new")
    class NewFormTests {

        @Test
        @DisplayName("deve exibir formulário de criação")
        void shouldShowCreateForm() throws Exception {
            mockMvc.perform(get("/products/new"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("products/form"))
                    .andExpect(model().attributeExists("product"));
        }
    }

    @Nested
    @DisplayName("GET /products/edit/{id}")
    class EditFormTests {

        @Test
        @DisplayName("deve exibir formulário de edição")
        void shouldShowEditForm() throws Exception {
            when(service.findById(1L)).thenReturn(createProduct(1L, "Produto A"));

            mockMvc.perform(get("/products/edit/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("products/form"))
                    .andExpect(model().attribute("product", hasProperty("name", is("Produto A"))));
        }

        @Test
        @DisplayName("deve redirecionar quando produto não existe")
        void shouldRedirectForNonExistentProduct() throws Exception {
            when(service.findById(999L)).thenThrow(new NoSuchElementException("Produto não encontrado"));

            mockMvc.perform(get("/products/edit/999"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/products"))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Nested
    @DisplayName("POST /products/save")
    class SaveTests {

        @Test
        @DisplayName("deve criar novo produto com dados válidos")
        void shouldCreateProduct() throws Exception {
            when(service.save(any())).thenReturn(createProduct(1L, "Novo Produto"));

            mockMvc.perform(post("/products/save")
                            .param("name", "Novo Produto")
                            .param("description", "Descrição")
                            .param("price", "99.99")
                            .param("quantity", "10"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/products"));
        }

        @Test
        @DisplayName("deve retornar formulário com erros de validação")
        void shouldReturnFormOnValidationErrors() throws Exception {
            mockMvc.perform(post("/products/save")
                            .param("name", "")
                            .param("price", "")
                            .param("quantity", ""))
                    .andExpect(status().isOk())
                    .andExpect(view().name("products/form"))
                    .andExpect(model().hasErrors());
        }

        @Test
        @DisplayName("deve atualizar produto existente")
        void shouldUpdateProduct() throws Exception {
            when(service.update(eq(1L), any())).thenReturn(createProduct(1L, "Atualizado"));

            mockMvc.perform(post("/products/save")
                            .param("id", "1")
                            .param("name", "Atualizado")
                            .param("description", "Desc")
                            .param("price", "150.00")
                            .param("quantity", "5"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/products"));
        }

        @Test
        @DisplayName("deve tratar exceção do service ao salvar")
        void shouldHandleServiceException() throws Exception {
            when(service.save(any())).thenThrow(new IllegalArgumentException("Erro de validação"));

            mockMvc.perform(post("/products/save")
                            .param("name", "Produto")
                            .param("price", "10.00")
                            .param("quantity", "1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("products/form"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @DisplayName("deve tratar exceção genérica ao salvar")
        void shouldHandleGenericException() throws Exception {
            when(service.save(any())).thenThrow(new RuntimeException("Erro inesperado"));

            mockMvc.perform(post("/products/save")
                            .param("name", "Produto")
                            .param("price", "10.00")
                            .param("quantity", "1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("products/form"))
                    .andExpect(model().attribute("error", "Erro inesperado ao salvar produto. Tente novamente."));
        }

        @Test
        @DisplayName("deve rejeitar preço negativo")
        void shouldRejectNegativePrice() throws Exception {
            mockMvc.perform(post("/products/save")
                            .param("name", "Produto")
                            .param("price", "-10.00")
                            .param("quantity", "1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("products/form"))
                    .andExpect(model().hasErrors());
        }

        @Test
        @DisplayName("deve rejeitar quantidade negativa")
        void shouldRejectNegativeQuantity() throws Exception {
            mockMvc.perform(post("/products/save")
                            .param("name", "Produto")
                            .param("price", "10.00")
                            .param("quantity", "-5"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("products/form"))
                    .andExpect(model().hasErrors());
        }
    }

    @Nested
    @DisplayName("POST /products/delete/{id}")
    class DeleteTests {

        @Test
        @DisplayName("deve excluir produto com sucesso")
        void shouldDeleteProduct() throws Exception {
            doNothing().when(service).delete(1L);

            mockMvc.perform(post("/products/delete/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/products"))
                    .andExpect(flash().attribute("success", "Produto excluído com sucesso!"));
        }

        @Test
        @DisplayName("deve tratar exclusão de produto inexistente")
        void shouldHandleDeleteNonExistent() throws Exception {
            doThrow(new NoSuchElementException("Produto não encontrado"))
                    .when(service).delete(999L);

            mockMvc.perform(post("/products/delete/999"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/products"))
                    .andExpect(flash().attributeExists("error"));
        }

        @Test
        @DisplayName("deve tratar erro genérico ao excluir")
        void shouldHandleGenericDeleteError() throws Exception {
            doThrow(new RuntimeException("Erro inesperado"))
                    .when(service).delete(1L);

            mockMvc.perform(post("/products/delete/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/products"))
                    .andExpect(flash().attribute("error", "Erro inesperado ao excluir produto."));
        }
    }

    @Test
    @DisplayName("deve redirecionar raiz para lista")
    void shouldRedirectRootToList() throws Exception {
        mockMvc.perform(get("/products/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));
    }
}
