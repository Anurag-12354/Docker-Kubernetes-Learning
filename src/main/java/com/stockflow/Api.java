package com.stockflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Request and response types. Entities are never returned directly. */
public final class Api {
    private Api() {}

    public record CreateProduct(
            @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9_-]+") String sku,
            @NotBlank @Size(max = 120) String name,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @NotNull @Min(0) @Max(1_000_000) Integer stock) {}

    public record AddStock(@NotNull @Min(1) @Max(1_000_000) Integer quantity) {}
    public record ProductView(Long id, String sku, String name, BigDecimal price, int stock) {}

    public record CreateOrder(@NotEmpty @Size(max = 20) List<@NotNull @Valid OrderLine> items) {}
    public record OrderLine(@NotNull @Positive Long productId,
                            @NotNull @Min(1) @Max(10_000) Integer quantity) {}
    public record ItemView(Long productId, String productName, BigDecimal unitPrice,
                           int quantity, BigDecimal lineTotal) {}
    public record OrderView(Long id, String status, Instant createdAt,
                            BigDecimal total, List<ItemView> items) {}
    public record PageView<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}
