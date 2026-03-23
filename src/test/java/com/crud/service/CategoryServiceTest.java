package com.crud.service;

import com.crud.model.Category;
import com.crud.repository.CategoryRepository;
import com.crud.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository repository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService service;

    private Category validCategory;

    @BeforeEach
    void setUp() {
        validCategory = new Category("Eletrônicos", "Produtos eletrônicos");
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("deve retornar lista vazia quando não há categorias")
        void shouldReturnEmptyList() {
            when(repository.findAll()).thenReturn(List.of());
            assertTrue(service.findAll().isEmpty());
        }

        @Test
        @DisplayName("deve retornar todas as categorias")
        void shouldReturnAllCategories() {
            var categories = List.of(validCategory, new Category("Roupas", "Vestuário"));
            when(repository.findAll()).thenReturn(categories);
            assertEquals(2, service.findAll().size());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("deve retornar categoria existente")
        void shouldReturnCategory() {
            when(repository.findById(1L)).thenReturn(Optional.of(validCategory));
            assertNotNull(service.findById(1L));
            assertEquals("Eletrônicos", service.findById(1L).getName());
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
        @DisplayName("deve salvar categoria válida")
        void shouldSaveValidCategory() {
            when(repository.save(any(Category.class))).thenReturn(validCategory);
            Category saved = service.save(validCategory);
            assertNotNull(saved);
            verify(repository).save(validCategory);
        }

        @Test
        @DisplayName("deve rejeitar categoria nula - fail early")
        void shouldRejectNullCategory() {
            assertThrows(IllegalArgumentException.class, () -> service.save(null));
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("deve atualizar categoria existente")
        void shouldUpdateExistingCategory() {
            validCategory.setId(1L);
            when(repository.findById(1L)).thenReturn(Optional.of(validCategory));
            when(repository.save(any())).thenReturn(validCategory);

            Category updated = new Category("Eletrônicos Pro", "Atualizado");
            Category result = service.update(1L, updated);

            assertNotNull(result);
            verify(repository).save(any());
        }

        @Test
        @DisplayName("deve lançar exceção ao atualizar com ID nulo - fail early")
        void shouldThrowForNullId() {
            assertThrows(IllegalArgumentException.class, () -> service.update(null, validCategory));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar exceção ao atualizar com categoria nula - fail early")
        void shouldThrowForNullCategory() {
            assertThrows(IllegalArgumentException.class, () -> service.update(1L, null));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar exceção ao atualizar categoria inexistente")
        void shouldThrowForNonExistentCategory() {
            when(repository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(NoSuchElementException.class, () -> service.update(999L, validCategory));
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("deve excluir categoria existente sem produtos vinculados")
        void shouldDeleteExistingCategory() {
            when(repository.existsById(1L)).thenReturn(true);
            when(productRepository.existsByCategoryId(1L)).thenReturn(false);
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

        @Test
        @DisplayName("deve impedir exclusão de categoria com produtos vinculados")
        void shouldPreventDeleteWhenProductsExist() {
            when(repository.existsById(1L)).thenReturn(true);
            when(productRepository.existsByCategoryId(1L)).thenReturn(true);
            assertThrows(IllegalStateException.class, () -> service.delete(1L));
            verify(repository, never()).deleteById(any());
        }
    }
}
