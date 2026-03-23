package com.crud.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Category validCategory() {
        return new Category("Eletrônicos", "Produtos eletrônicos em geral");
    }

    @Test
    @DisplayName("Categoria válida não deve ter violações")
    void validCategoryShouldHaveNoViolations() {
        Set<ConstraintViolation<Category>> violations = validator.validate(validCategory());
        assertTrue(violations.isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Nome inválido deve gerar violação")
    void invalidNameShouldHaveViolation(String name) {
        Category c = validCategory();
        c.setName(name);
        assertFalse(validator.validate(c).isEmpty());
    }

    @Test
    @DisplayName("Nome com mais de 100 caracteres deve gerar violação")
    void longNameShouldHaveViolation() {
        Category c = validCategory();
        c.setName("A".repeat(101));
        assertFalse(validator.validate(c).isEmpty());
    }

    @Test
    @DisplayName("Descrição com mais de 500 caracteres deve gerar violação")
    void longDescriptionShouldHaveViolation() {
        Category c = validCategory();
        c.setDescription("A".repeat(501));
        assertFalse(validator.validate(c).isEmpty());
    }

    @Test
    @DisplayName("Construtor padrão deve criar categoria vazia")
    void defaultConstructorShouldCreateEmptyCategory() {
        Category c = new Category();
        assertNull(c.getId());
        assertNull(c.getName());
        assertNull(c.getDescription());
    }

    @Test
    @DisplayName("Getters e setters devem funcionar corretamente")
    void gettersAndSettersShouldWork() {
        Category c = new Category();
        c.setId(1L);
        c.setName("Teste");
        c.setDescription("Desc");

        assertEquals(1L, c.getId());
        assertEquals("Teste", c.getName());
        assertEquals("Desc", c.getDescription());
    }
}
