package com.crud.service;

import com.crud.model.Product;
import com.crud.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductService service;

    private Product validProduct;

    @BeforeEach
    void setUp() {
        validProduct = new Product("Notebook", "Notebook Dell 15\"", new BigDecimal("2999.99"), 10);
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("deve retornar lista vazia quando não há produtos")
        void shouldReturnEmptyList() {
            when(repository.findAll()).thenReturn(List.of());
            assertTrue(service.findAll().isEmpty());
        }

        @Test
        @DisplayName("deve retornar todos os produtos")
        void shouldReturnAllProducts() {
            var products = List.of(validProduct, new Product("Mouse", "Mouse USB", new BigDecimal("49.90"), 50));
            when(repository.findAll()).thenReturn(products);
            assertEquals(2, service.findAll().size());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("deve retornar produto existente")
        void shouldReturnProduct() {
            when(repository.findById(1L)).thenReturn(Optional.of(validProduct));
            assertNotNull(service.findById(1L));
            assertEquals("Notebook", service.findById(1L).getName());
        }

        @Test
        @DisplayName("deve lançar exceção para ID inexistente")
        void shouldThrowForNonExistentId() {
            when(repository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(NoSuchElementException.class, () -> service.findById(999L));
        }

        @Test
        @DisplayName("deve lançar exceção para ID nulo - fail early")
        void shouldThrowForNullId() {
            assertThrows(IllegalArgumentException.class, () -> service.findById(null));
            verify(repository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("save")
    class SaveTests {

        @Test
        @DisplayName("deve salvar produto válido")
        void shouldSaveValidProduct() {
            when(repository.save(any(Product.class))).thenReturn(validProduct);
            Product saved = service.save(validProduct);
            assertNotNull(saved);
            verify(repository).save(validProduct);
        }

        @Test
        @DisplayName("deve rejeitar produto nulo - fail early")
        void shouldRejectNullProduct() {
            assertThrows(IllegalArgumentException.class, () -> service.save(null));
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("deve atualizar produto existente")
        void shouldUpdateExistingProduct() {
            validProduct.setId(1L);
            when(repository.findById(1L)).thenReturn(Optional.of(validProduct));
            when(repository.save(any())).thenReturn(validProduct);

            Product updated = new Product("Notebook Pro", "Atualizado", new BigDecimal("3999.99"), 5);
            Product result = service.update(1L, updated);

            assertNotNull(result);
            verify(repository).save(any());
        }

        @Test
        @DisplayName("deve lançar exceção ao atualizar com ID nulo - fail early")
        void shouldThrowForNullId() {
            assertThrows(IllegalArgumentException.class, () -> service.update(null, validProduct));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar exceção ao atualizar com produto nulo - fail early")
        void shouldThrowForNullProduct() {
            assertThrows(IllegalArgumentException.class, () -> service.update(1L, null));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar exceção ao atualizar produto inexistente")
        void shouldThrowForNonExistentProduct() {
            when(repository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(NoSuchElementException.class, () -> service.update(999L, validProduct));
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("deve excluir produto existente")
        void shouldDeleteExistingProduct() {
            when(repository.existsById(1L)).thenReturn(true);
            assertDoesNotThrow(() -> service.delete(1L));
            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("deve lançar exceção ao excluir ID inexistente")
        void shouldThrowForNonExistentId() {
            when(repository.existsById(999L)).thenReturn(false);
            assertThrows(NoSuchElementException.class, () -> service.delete(999L));
        }

        @Test
        @DisplayName("deve lançar exceção ao excluir com ID nulo - fail early")
        void shouldThrowForNullId() {
            assertThrows(IllegalArgumentException.class, () -> service.delete(null));
            verify(repository, never()).deleteById(any());
        }
    }
}
