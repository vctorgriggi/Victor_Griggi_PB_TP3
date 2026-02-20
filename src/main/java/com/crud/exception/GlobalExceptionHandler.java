package com.crud.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.NoSuchElementException;

// Fail gracefully: captura exceções não tratadas e exibe página de erro amigável
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNotFound(NoSuchElementException e, Model model) {
        model.addAttribute("errorTitle", "Recurso não encontrado");
        model.addAttribute("errorMessage", e.getMessage());
        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleBadRequest(IllegalArgumentException e, Model model) {
        model.addAttribute("errorTitle", "Dados inválidos");
        model.addAttribute("errorMessage", e.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericError(Exception e, Model model) {
        // Não expõe detalhes internos ao usuário (segurança)
        model.addAttribute("errorTitle", "Erro interno");
        model.addAttribute("errorMessage", "Ocorreu um erro inesperado. Tente novamente mais tarde.");
        return "error";
    }
}
