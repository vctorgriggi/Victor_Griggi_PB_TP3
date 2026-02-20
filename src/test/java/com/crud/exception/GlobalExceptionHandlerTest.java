package com.crud.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Deve tratar NoSuchElementException")
    void shouldHandleNotFound() {
        Model model = new ExtendedModelMap();
        String view = handler.handleNotFound(new NoSuchElementException("Produto não encontrado"), model);

        assertEquals("error", view);
        assertEquals("Recurso não encontrado", model.getAttribute("errorTitle"));
        assertEquals("Produto não encontrado", model.getAttribute("errorMessage"));
    }

    @Test
    @DisplayName("Deve tratar IllegalArgumentException")
    void shouldHandleBadRequest() {
        Model model = new ExtendedModelMap();
        String view = handler.handleBadRequest(new IllegalArgumentException("Dados inválidos"), model);

        assertEquals("error", view);
        assertEquals("Dados inválidos", model.getAttribute("errorTitle"));
        assertEquals("Dados inválidos", model.getAttribute("errorMessage"));
    }

    @Test
    @DisplayName("Deve tratar exceção genérica sem expor detalhes")
    void shouldHandleGenericError() {
        Model model = new ExtendedModelMap();
        String view = handler.handleGenericError(new RuntimeException("Erro interno secreto"), model);

        assertEquals("error", view);
        assertEquals("Erro interno", model.getAttribute("errorTitle"));
        // Não deve conter a mensagem interna
        assertFalse(model.getAttribute("errorMessage").toString().contains("secreto"));
    }
}
