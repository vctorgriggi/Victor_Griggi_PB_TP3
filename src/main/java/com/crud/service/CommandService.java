package com.crud.service;

// operações de escrita separadas de consultas (command-query separation)
public interface CommandService<T, ID> {
    T save(T entity);
    T update(ID id, T entity);
    void delete(ID id);
}
