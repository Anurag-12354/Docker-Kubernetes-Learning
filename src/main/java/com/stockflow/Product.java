package com.stockflow;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, unique = true, length = 64)
    String sku;
    @Column(nullable = false, length = 120)
    String name;
    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal price;
    @Column(nullable = false)
    int stock;

    protected Product() {}

    Product(String sku, String name, BigDecimal price, int stock) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }
}
