package com.stockflow;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Embeddable
class OrderItem {
    @Column(name = "product_id", nullable = false)
    Long productId;
    @Column(name = "product_name", nullable = false, length = 120)
    String productName;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    BigDecimal unitPrice;
    @Column(nullable = false)
    int quantity;

    protected OrderItem() {}

    OrderItem(Product product, int quantity) {
        this.productId = product.id;
        this.productName = product.name;
        this.unitPrice = product.price;
        this.quantity = quantity;
    }
}
