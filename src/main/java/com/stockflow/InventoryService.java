package com.stockflow;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import static com.stockflow.Api.*;

@Service
@Transactional(readOnly = true)
public class InventoryService {
    private final ProductRepository products;
    private final OrderRepository orders;

    public InventoryService(ProductRepository products, OrderRepository orders) {
        this.products = products;
        this.orders = orders;
    }

    @Transactional
    public ProductView createProduct(CreateProduct input) {
        Product product = new Product(input.sku().toUpperCase(Locale.ROOT), input.name().trim(),
                input.price(), input.stock());
        return view(products.saveAndFlush(product));
    }

    public PageView<ProductView> listProducts(int page, int size) {
        var result = products.findAll(pageRequest(page, size));
        return new PageView<>(result.map(InventoryService::view).getContent(), page, size,
                result.getTotalElements(), result.getTotalPages());
    }

    public ProductView getProduct(long id) {
        return view(products.findById(id).orElseThrow(() -> missing("Product", id)));
    }

    @Transactional
    public ProductView addStock(long id, AddStock input) {
        Product product = lockedProduct(id);
        try {
            product.stock = Math.addExact(product.stock, input.quantity());
        } catch (ArithmeticException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "Stock exceeds supported range");
        }
        return view(product);
    }

    @Transactional
    public OrderView placeOrder(CreateOrder input) {
        // Consistent lock ordering prevents two multi-product orders locking in opposite order.
        SortedMap<Long, Integer> quantities = new TreeMap<>();
        for (OrderLine line : input.items()) {
            if (quantities.putIfAbsent(line.productId(), line.quantity()) != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Each product may appear only once");
            }
        }
        PurchaseOrder order = new PurchaseOrder();
        order.total = BigDecimal.ZERO;
        for (var entry : quantities.entrySet()) {
            Product product = lockedProduct(entry.getKey());
            int quantity = entry.getValue();
            if (product.stock < quantity) {
                throw new ApiException(HttpStatus.CONFLICT, "Insufficient stock for product " + product.id);
            }
            product.stock -= quantity;
            OrderItem item = new OrderItem(product, quantity);
            order.items.add(item);
            order.total = order.total.add(item.unitPrice.multiply(BigDecimal.valueOf(quantity)));
        }
        // Failure anywhere in this transaction rolls back every stock change.
        return view(orders.saveAndFlush(order));
    }

    public OrderView getOrder(long id) {
        return view(orders.findById(id).orElseThrow(() -> missing("Order", id)));
    }

    public PageView<OrderView> listOrders(int page, int size) {
        var result = orders.findAll(pageRequest(page, size));
        return new PageView<>(result.map(InventoryService::view).getContent(), page, size,
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public OrderView cancelOrder(long id) {
        PurchaseOrder order = orders.findForUpdate(id).orElseThrow(() -> missing("Order", id));
        // Locking the order makes repeated/concurrent cancellation restore stock only once.
        if (order.status == PurchaseOrder.Status.CANCELLED) return view(order);
        List<OrderItem> sorted = order.items.stream()
                .sorted(Comparator.comparing(item -> item.productId)).toList();
        for (OrderItem item : sorted) {
            Product product = lockedProduct(item.productId);
            try {
                product.stock = Math.addExact(product.stock, item.quantity);
            } catch (ArithmeticException ex) {
                throw new ApiException(HttpStatus.CONFLICT, "Stock exceeds supported range");
            }
        }
        order.status = PurchaseOrder.Status.CANCELLED;
        return view(order);
    }

    private Product lockedProduct(long id) {
        return products.findForUpdate(id).orElseThrow(() -> missing("Product", id));
    }

    private static PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Use page >= 0 and size between 1 and 100");
        }
        return PageRequest.of(page, size, Sort.by("id").ascending());
    }

    private static ApiException missing(String kind, long id) {
        return new ApiException(HttpStatus.NOT_FOUND, kind + " " + id + " does not exist");
    }

    private static ProductView view(Product p) {
        return new ProductView(p.id, p.sku, p.name, p.price, p.stock);
    }

    private static OrderView view(PurchaseOrder o) {
        List<ItemView> items = o.items.stream().map(i -> new ItemView(i.productId, i.productName,
                i.unitPrice, i.quantity, i.unitPrice.multiply(BigDecimal.valueOf(i.quantity)))).toList();
        return new OrderView(o.id, o.status.name(), o.createdAt, o.total, items);
    }
}
