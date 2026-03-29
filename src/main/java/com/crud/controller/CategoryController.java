package com.crud.controller;

import com.crud.model.Category;
import com.crud.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", service.findAll());
        return "categories/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("category", new Category());
        return "categories/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("category", service.findById(id));
            return "categories/form";
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/categories";
        }
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Category category, BindingResult result,
                       Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "categories/form";
        }

        try {
            if (category.getId() != null) {
                service.update(category.getId(), category);
                redirectAttributes.addFlashAttribute("success", "Categoria atualizada com sucesso!");
            } else {
                service.save(category);
                redirectAttributes.addFlashAttribute("success", "Categoria cadastrada com sucesso!");
            }
            return "redirect:/categories";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "categories/form";
        } catch (Exception e) {
            model.addAttribute("error", "Erro inesperado ao salvar categoria. Tente novamente.");
            return "categories/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            service.delete(id);
            redirectAttributes.addFlashAttribute("success", "Categoria excluída com sucesso!");
        } catch (NoSuchElementException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erro inesperado ao excluir categoria.");
        }
        return "redirect:/categories";
    }
}
