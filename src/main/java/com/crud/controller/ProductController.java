package com.crud.controller;

import com.crud.model.Product;
import com.crud.service.CategoryService;
import com.crud.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService service;
    private final CategoryService categoryService;

    public ProductController(ProductService service, CategoryService categoryService) {
        this.service = service;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", service.findAll());
        return "products/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.findAll());
        return "products/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("product", service.findById(id));
            model.addAttribute("categories", categoryService.findAll());
            return "products/form";
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/products";
        }
    }

    // Fail gracefully: captura erros de validação e retorna feedback ao usuário
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Product product, BindingResult result,
                       @RequestParam(required = false) Long categoryId,
                       Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            return "products/form";
        }

        try {
            if (categoryId != null) {
                product.setCategory(categoryService.findById(categoryId));
            } else {
                product.setCategory(null);
            }

            if (product.getId() != null) {
                service.update(product.getId(), product);
                redirectAttributes.addFlashAttribute("success", "Produto atualizado com sucesso!");
            } else {
                service.save(product);
                redirectAttributes.addFlashAttribute("success", "Produto cadastrado com sucesso!");
            }
            return "redirect:/products";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.findAll());
            return "products/form";
        } catch (Exception e) {
            model.addAttribute("error", "Erro inesperado ao salvar produto. Tente novamente.");
            model.addAttribute("categories", categoryService.findAll());
            return "products/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            service.delete(id);
            redirectAttributes.addFlashAttribute("success", "Produto excluído com sucesso!");
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erro inesperado ao excluir produto.");
        }
        return "redirect:/products";
    }

    // Redireciona raiz para lista de produtos
    @GetMapping("/")
    public String redirectToList() {
        return "redirect:/products";
    }
}
