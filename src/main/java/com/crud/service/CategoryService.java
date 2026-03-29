package com.crud.service;

import com.crud.model.Category;
import com.crud.repository.CategoryRepository;
import com.crud.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CategoryService implements QueryService<Category, Long>, CommandService<Category, Long> {

    private final CategoryRepository repository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository repository, ProductRepository productRepository) {
        this.repository = repository;
        this.productRepository = productRepository;
    }

    @Override
    public List<Category> findAll() {
        return repository.findAll();
    }

    @Override
    public Category findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID não pode ser nulo");
        }
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Categoria não encontrada com ID: " + id));
    }

    @Override
    public Category save(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Categoria não pode ser nula");
        }
        return repository.save(category);
    }

    @Override
    public Category update(Long id, Category updatedCategory) {
        if (id == null) {
            throw new IllegalArgumentException("ID não pode ser nulo");
        }
        if (updatedCategory == null) {
            throw new IllegalArgumentException("Categoria não pode ser nula");
        }

        Category existing = findById(id);
        existing.setName(updatedCategory.getName());
        existing.setDescription(updatedCategory.getDescription());
        return repository.save(existing);
    }

    @Override
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID não pode ser nulo");
        }
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Categoria não encontrada com ID: " + id);
        }
        if (productRepository.existsByCategoryId(id)) {
            throw new IllegalStateException("Não é possível excluir categoria com produtos vinculados");
        }
        repository.deleteById(id);
    }
}
