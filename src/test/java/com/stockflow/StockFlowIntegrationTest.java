package com.stockflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Runs unchanged against H2, or PostgreSQL when TEST_DATABASE_URL is set. */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("test")
class StockFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired InventoryService service;

    private String sku() { return "SKU-" + UUID.randomUUID(); }

    private long product(String price, int stock) throws Exception {
        var result = mvc.perform(post("/api/products").contentType("application/json")
                .content(json.writeValueAsString(Map.of("sku", sku(), "name", "Keyboard",
                        "price", new BigDecimal(price), "stock", stock))))
                .andExpect(status().isCreated()).andReturn();
        return body(result).get("id").asLong();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    private String orderBody(long id, int quantity) throws Exception {
        return json.writeValueAsString(Map.of("items", List.of(Map.of("productId", id, "quantity", quantity))));
    }

    @Test void placesOrderAndRestoresStockOnlyOnce() throws Exception {
        long id = product("19.95", 10);
        var placed = mvc.perform(post("/api/orders").contentType("application/json").content(orderBody(id, 3)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(59.85)).andReturn();
        long order = body(placed).get("id").asLong();
        mvc.perform(get("/api/products/" + id)).andExpect(jsonPath("$.stock").value(7));
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/orders/" + order + "/cancel"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        }
        mvc.perform(get("/api/products/" + id)).andExpect(jsonPath("$.stock").value(10));
    }

    @Test void rejectsInsufficientStock() throws Exception {
        long id = product("5.00", 1);
        mvc.perform(post("/api/orders").contentType("application/json").content(orderBody(id, 2)))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/products/" + id)).andExpect(jsonPath("$.stock").value(1));
    }

    @Test void rollsBackEarlierStockChangesWhenLaterItemFails() throws Exception {
        long first = product("5.00", 5);
        long second = product("7.00", 0);
        String request = json.writeValueAsString(Map.of("items", List.of(
                Map.of("productId", first, "quantity", 2), Map.of("productId", second, "quantity", 1))));
        mvc.perform(post("/api/orders").contentType("application/json").content(request))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/products/" + first)).andExpect(jsonPath("$.stock").value(5));
    }

    @Test void rejectsInvalidBodiesAndQuantities() throws Exception {
        for (String request : List.of("{}", "{\"items\":[]}", "{\"items\":[null]}",
                "{\"items\":[{\"productId\":1,\"quantity\":0}]}",
                "{\"items\":[{\"productId\":1,\"quantity\":1.5}]}", "{bad json")) {
            mvc.perform(post("/api/orders").contentType("application/json").content(request))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/products").contentType("application/json")
                .content("{\"sku\":\"BAD\",\"name\":\"Item\",\"price\":-1,\"stock\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test void rejectsDuplicateSkuIgnoringCase() throws Exception {
        String sku = sku();
        String request = json.writeValueAsString(Map.of("sku", sku, "name", "Item", "price", 2, "stock", 0));
        mvc.perform(post("/api/products").contentType("application/json").content(request))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/products").contentType("application/json")
                .content(request.replace(sku, sku.toLowerCase(Locale.ROOT))))
                .andExpect(status().isConflict());
    }

    @Test void rejectsDuplicateProductsWithinAnOrder() throws Exception {
        long id = product("5.00", 10);
        String request = json.writeValueAsString(Map.of("items", List.of(
                Map.of("productId", id, "quantity", 1), Map.of("productId", id, "quantity", 2))));
        mvc.perform(post("/api/orders").contentType("application/json").content(request))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/products/" + id)).andExpect(jsonPath("$.stock").value(10));
    }

    @Test void handlesMissingResourcesAndPagination() throws Exception {
        mvc.perform(get("/api/products/9223372036854775807")).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/9223372036854775807")).andExpect(status().isNotFound());
        mvc.perform(get("/api/products?page=-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/orders?size=101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products?size=1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1));
    }

    @Test void acceptsLargestSupportedPriceAndQuantityWithoutLosingPrecision() throws Exception {
        long id = product("9999999999.99", 10000);
        mvc.perform(post("/api/orders").contentType("application/json").content(orderBody(id, 10000)))
                .andExpect(status().isCreated());
        assertThat(service.getProduct(id).stock()).isZero();
    }

    @Test void replenishesStock() throws Exception {
        long id = product("3.00", 0);
        mvc.perform(post("/api/products/" + id + "/stock").contentType("application/json")
                .content("{\"quantity\":5}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(5));
    }

    @Test void exposesHealthAndDeploymentInformation() throws Exception {
        for (String path : List.of("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness")) {
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        }
        mvc.perform(get("/api/info")).andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("StockFlow"));
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
    }

    @Test void concurrentOrdersCannotOversellTheLastItem() throws Exception {
        long id = product("9.00", 1);
        List<Integer> results = concurrently(2, () -> {
            try {
                service.placeOrder(new Api.CreateOrder(List.of(new Api.OrderLine(id, 1))));
                return 201;
            } catch (ApiException ex) { return ex.status.value(); }
        });
        assertThat(results).containsExactlyInAnyOrder(201, 409);
        assertThat(service.getProduct(id).stock()).isZero();
    }

    @Test void concurrentCancellationRestoresStockOnce() throws Exception {
        long id = product("9.00", 4);
        long orderId = service.placeOrder(new Api.CreateOrder(List.of(new Api.OrderLine(id, 3)))).id();
        concurrently(2, () -> service.cancelOrder(orderId));
        assertThat(service.getProduct(id).stock()).isEqualTo(4);
    }

    private <T> List<T> concurrently(int count, Callable<T> action) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Start barrier timed out");
                    return action.call();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) results.add(future.get(20, TimeUnit.SECONDS));
            return results;
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }
}
