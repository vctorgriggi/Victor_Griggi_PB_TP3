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

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Product validProduct() {
        return new Product("Notebook", "Dell Inspiron", new BigDecimal("2500.00"), 10);
    }

    @Test
    @DisplayName("Produto válido não deve ter violações")
    void validProductShouldHaveNoViolations() {
        Set<ConstraintViolation<Product>> violations = validator.validate(validProduct());
        assertTrue(violations.isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Nome inválido deve gerar violação")
    void invalidNameShouldHaveViolation(String name) {
        Product p = validProduct();
        p.setName(name);
        assertFalse(validator.validate(p).isEmpty());
    }

    @Test
    @DisplayName("Nome com mais de 100 caracteres deve gerar violação")
    void longNameShouldHaveViolation() {
        Product p = validProduct();
        p.setName("A".repeat(101));
        assertFalse(validator.validate(p).isEmpty());
    }

    @Test
    @DisplayName("Descrição com mais de 500 caracteres deve gerar violação")
    void longDescriptionShouldHaveViolation() {
        Product p = validProduct();
        p.setDescription("A".repeat(501));
        assertFalse(validator.validate(p).isEmpty());
    }

    @Test
    @DisplayName("Preço nulo deve gerar violação")
    void nullPriceShouldHaveViolation() {
        Product p = validProduct();
        p.setPrice(null);
        assertFalse(validator.validate(p).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0, -100.0})
    @DisplayName("Preço não positivo deve gerar violação")
    void nonPositivePriceShouldHaveViolation(double price) {
        Product p = validProduct();
        p.setPrice(BigDecimal.valueOf(price));
        assertFalse(validator.validate(p).isEmpty());
    }

    @Test
    @DisplayName("Quantidade nula deve gerar violação")
    void nullQuantityShouldHaveViolation() {
        Product p = validProduct();
        p.setQuantity(null);
        assertFalse(validator.validate(p).isEmpty());
    }

    @Test
    @DisplayName("Quantidade negativa deve gerar violação")
    void negativeQuantityShouldHaveViolation() {
        Product p = validProduct();
        p.setQuantity(-1);
        assertFalse(validator.validate(p).isEmpty());
    }

    @Test
    @DisplayName("Quantidade zero deve ser válida")
    void zeroQuantityShouldBeValid() {
        Product p = validProduct();
        p.setQuantity(0);
        assertTrue(validator.validate(p).isEmpty());
    }

    @Test
    @DisplayName("Construtor padrão deve criar produto vazio")
    void defaultConstructorShouldCreateEmptyProduct() {
        Product p = new Product();
        assertNull(p.getId());
        assertNull(p.getName());
        assertNull(p.getDescription());
        assertNull(p.getPrice());
        assertNull(p.getQuantity());
    }

    @Test
    @DisplayName("Getters e setters devem funcionar corretamente")
    void gettersAndSettersShouldWork() {
        Product p = new Product();
        p.setId(1L);
        p.setName("Teste");
        p.setDescription("Desc");
        p.setPrice(new BigDecimal("10.00"));
        p.setQuantity(5);

        assertEquals(1L, p.getId());
        assertEquals("Teste", p.getName());
        assertEquals("Desc", p.getDescription());
        assertEquals(new BigDecimal("10.00"), p.getPrice());
        assertEquals(5, p.getQuantity());
    }
}
