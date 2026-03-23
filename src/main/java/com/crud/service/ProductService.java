package com.crud.service;

import com.crud.model.Product;
import com.crud.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ProductService implements CrudService<Product, Long> {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Product> findAll() {
        return repository.findAll();
    }

    @Override
    public Product findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID não pode ser nulo");
        }
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado com ID: " + id));
    }

    // Fail early: rejeita objeto nulo antes de acessar o repositório
    @Override
    public Product save(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Produto não pode ser nulo");
        }
        return repository.save(product);
    }

    @Override
    public Product update(Long id, Product updatedProduct) {
        if (id == null) {
            throw new IllegalArgumentException("ID não pode ser nulo");
        }
        if (updatedProduct == null) {
            throw new IllegalArgumentException("Produto não pode ser nulo");
        }

        Product existing = findById(id);
        existing.setName(updatedProduct.getName());
        existing.setDescription(updatedProduct.getDescription());
        existing.setPrice(updatedProduct.getPrice());
        existing.setQuantity(updatedProduct.getQuantity());
        existing.setCategory(updatedProduct.getCategory());
        return repository.save(existing);
    }

    @Override
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID não pode ser nulo");
        }
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Produto não encontrado com ID: " + id);
        }
        repository.deleteById(id);
    }
}
