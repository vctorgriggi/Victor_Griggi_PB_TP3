package com.crud.service;

import java.util.List;

// consultas separadas de modificadores (command-query separation)
public interface QueryService<T, ID> {
    List<T> findAll();
    T findById(ID id);
}
