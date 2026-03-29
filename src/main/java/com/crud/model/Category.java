package com.crud.model;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    public Category() {}

    public Category(String name, String description) {
        super(name, description);
    }
}
