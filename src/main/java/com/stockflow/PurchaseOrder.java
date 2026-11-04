package com.stockflow;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
class PurchaseOrder {
    enum Status { PLACED, CANCELLED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    Status status = Status.PLACED;
    @Column(name = "created_at", nullable = false)
    Instant createdAt = Instant.now();
    @Column(nullable = false, precision = 20, scale = 2)
    BigDecimal total;
    @ElementCollection
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "item_index")
    List<OrderItem> items = new ArrayList<>();

    protected PurchaseOrder() {}
}
