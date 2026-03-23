package com.crud.integration;

import com.crud.model.Category;
import com.crud.model.Product;
import com.crud.repository.CategoryRepository;
import com.crud.repository.ProductRepository;
import com.crud.service.CategoryService;
import com.crud.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductCategoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @BeforeEach
    void cleanUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Nested
    @DisplayName("Integração Product-Category via service")
    class ServiceIntegrationTests {

        @Test
        @DisplayName("deve criar produto com categoria")
        void shouldCreateProductWithCategory() {
            Category category = categoryService.save(new Category("Eletrônicos", "Produtos eletrônicos"));
            Product product = new Product("Notebook", "Dell", new BigDecimal("2999.99"), 5);
            product.setCategory(category);

            Product saved = productService.save(product);

            assertNotNull(saved.getId());
            assertNotNull(saved.getCategory());
            assertEquals("Eletrônicos", saved.getCategory().getName());
        }

        @Test
        @DisplayName("deve criar produto sem categoria")
        void shouldCreateProductWithoutCategory() {
            Product product = new Product("Mouse", "USB", new BigDecimal("49.90"), 100);
            Product saved = productService.save(product);

            assertNotNull(saved.getId());
            assertNull(saved.getCategory());
        }

        @Test
        @DisplayName("deve impedir exclusão de categoria com produtos vinculados")
        void shouldPreventCategoryDeletionWithProducts() {
            Category category = categoryService.save(new Category("Eletrônicos", "Desc"));
            Product product = new Product("Notebook", "Dell", new BigDecimal("2999.99"), 5);
            product.setCategory(category);
            productService.save(product);

            assertThrows(IllegalStateException.class, () -> categoryService.delete(category.getId()));
        }

        @Test
        @DisplayName("deve permitir exclusão de categoria sem produtos")
        void shouldAllowCategoryDeletionWithoutProducts() {
            Category category = categoryService.save(new Category("Vazia", "Sem produtos"));
            assertDoesNotThrow(() -> categoryService.delete(category.getId()));
            assertTrue(categoryRepository.findById(category.getId()).isEmpty());
        }

        @Test
        @DisplayName("deve atualizar categoria do produto")
        void shouldUpdateProductCategory() {
            Category cat1 = categoryService.save(new Category("Eletrônicos", "Desc"));
            Category cat2 = categoryService.save(new Category("Periféricos", "Desc"));

            Product product = new Product("Mouse", "USB", new BigDecimal("49.90"), 100);
            product.setCategory(cat1);
            Product saved = productService.save(product);

            saved.setCategory(cat2);
            Product updated = productService.update(saved.getId(), saved);

            assertEquals("Periféricos", updated.getCategory().getName());
        }
    }

    @Nested
    @DisplayName("Integração Product-Category via HTTP")
    class HttpIntegrationTests {

        @Test
        @DisplayName("deve criar produto com categoria via formulário")
        void shouldCreateProductWithCategoryViaForm() throws Exception {
            Category category = categoryRepository.save(new Category("Eletrônicos", "Desc"));

            mockMvc.perform(post("/products/save")
                            .param("name", "Notebook")
                            .param("description", "Dell")
                            .param("price", "2999.99")
                            .param("quantity", "5")
                            .param("categoryId", category.getId().toString()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/products"));

            Product saved = productRepository.findAll().get(0);
            assertNotNull(saved.getCategory());
            assertEquals(category.getId(), saved.getCategory().getId());
        }

        @Test
        @DisplayName("deve listar categorias na página de novo produto")
        void shouldListCategoriesOnProductForm() throws Exception {
            categoryRepository.save(new Category("Eletrônicos", "Desc"));
            categoryRepository.save(new Category("Roupas", "Desc"));

            mockMvc.perform(get("/products/new"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("categories"));
        }

        @Test
        @DisplayName("deve impedir exclusão de categoria com produtos via HTTP")
        void shouldPreventCategoryDeletionViaHttp() throws Exception {
            Category category = categoryRepository.save(new Category("Eletrônicos", "Desc"));
            Product product = new Product("Notebook", "Dell", new BigDecimal("2999.99"), 5);
            product.setCategory(category);
            productRepository.save(product);

            mockMvc.perform(post("/categories/delete/" + category.getId()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("error"));

            assertTrue(categoryRepository.findById(category.getId()).isPresent());
        }

        @Test
        @DisplayName("deve criar produto sem categoria via formulário")
        void shouldCreateProductWithoutCategoryViaForm() throws Exception {
            mockMvc.perform(post("/products/save")
                            .param("name", "Mouse")
                            .param("description", "USB")
                            .param("price", "49.90")
                            .param("quantity", "100"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/products"));

            Product saved = productRepository.findAll().get(0);
            assertNull(saved.getCategory());
        }
    }
}
