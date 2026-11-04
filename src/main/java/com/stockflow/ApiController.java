package com.stockflow;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.Map;
import static com.stockflow.Api.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final InventoryService service;
    private final String version;
    private final String instance;

    public ApiController(InventoryService service,
                         @Value("${app.version}") String version,
                         @Value("${app.instance}") String instance) {
        this.service = service;
        this.version = version;
        this.instance = instance;
    }

    @GetMapping("/info")
    public Map<String, String> info() {
        return Map.of("application", "StockFlow", "version", version, "instance", instance);
    }

    @PostMapping("/products")
    public ResponseEntity<ProductView> createProduct(@Valid @RequestBody CreateProduct input) {
        ProductView product = service.createProduct(input);
        return ResponseEntity.created(URI.create("/api/products/" + product.id())).body(product);
    }

    @GetMapping("/products")
    public PageView<ProductView> products(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return service.listProducts(page, size);
    }

    @GetMapping("/products/{id}")
    public ProductView product(@PathVariable long id) { return service.getProduct(id); }

    @PostMapping("/products/{id}/stock")
    public ProductView stock(@PathVariable long id, @Valid @RequestBody AddStock input) {
        return service.addStock(id, input);
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderView> createOrder(@Valid @RequestBody CreateOrder input) {
        OrderView order = service.placeOrder(input);
        return ResponseEntity.created(URI.create("/api/orders/" + order.id())).body(order);
    }

    @GetMapping("/orders")
    public PageView<OrderView> orders(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return service.listOrders(page, size);
    }

    @GetMapping("/orders/{id}")
    public OrderView order(@PathVariable long id) { return service.getOrder(id); }

    @PostMapping("/orders/{id}/cancel")
    public OrderView cancel(@PathVariable long id) { return service.cancelOrder(id); }
}
