# StockFlow

A working Spring Boot order and inventory backend for learning Docker and Kubernetes.
The application code is provided so you can focus on packaging, networking,
persistence, scaling, deployments and troubleshooting.

## Requirements

- JDK 17 or 21 (`java -version`). The project targets Java 17 and runs on Java 21.
- Internet access on the first build. Maven is downloaded by the included wrapper.
- PostgreSQL for persistent operation; the `demo` profile needs no database installation.

## Start immediately on Windows PowerShell

Extract the ZIP and open a terminal **inside the `stockflow` folder**, where `pom.xml` lives.

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

On Linux/macOS:

```bash
chmod +x mvnw
./mvnw test
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Check these URLs:

- http://localhost:8080/actuator/health — `{"status":"UP"}`
- http://localhost:8080/api/info — application version and instance name
- http://localhost:8080/api/products — initially an empty page

The demo profile uses **in-memory H2**, not PostgreSQL. Data disappears when the
application stops. Use it to check that the backend works before our Docker lessons.
Use the shared PostgreSQL database when running multiple backend replicas; each
demo JVM has its own separate database.
If port 8080 is occupied, set `$env:PORT="8081"` before starting (PowerShell).
The `/` URL does not have a web page; use the API URLs above.

## Try the main flow in a second PowerShell window

```powershell
$base = "http://localhost:8080"

$product = Invoke-RestMethod -Method Post -Uri "$base/api/products" `
  -ContentType "application/json" `
  -Body (@{sku="KEYBOARD-001"; name="Keyboard"; price=29.90; stock=10} | ConvertTo-Json)
$product

$orderBody = @{items=@(@{productId=$product.id; quantity=2})} | ConvertTo-Json -Depth 5
$order = Invoke-RestMethod -Method Post -Uri "$base/api/orders" `
  -ContentType "application/json" -Body $orderBody
$order | ConvertTo-Json -Depth 5

# Stock is now 8. The order total is 59.80 EUR.
Invoke-RestMethod "$base/api/products/$($product.id)"

# Cancel the order: stock returns to 10.
Invoke-RestMethod -Method Post "$base/api/orders/$($order.id)/cancel"
Invoke-RestMethod "$base/api/products/$($product.id)"
```

If you repeat the product creation, choose a new SKU or restart the demo application.
A duplicate SKU returns HTTP 409 intentionally. See `requests.http` for additional
requests usable with an IDE HTTP client.

## What is implemented

- Products: create, list, retrieve, replenish stock; case-insensitive unique SKU.
- Orders: place, list, retrieve and cancel; prices and product names are snapshots.
- Money uses `BigDecimal`; amounts are treated as EUR throughout this sample.
- Insufficient stock returns HTTP 409; missing resources return 404; invalid input returns 400.
- Stock deductions and order creation occur in one database transaction.
- Database row locks coordinate inventory updates across application instances.
- Products are locked in ascending ID order to reduce deadlock risk.
- An order row lock makes repeated/concurrent cancellation restore stock only once.
- Flyway creates the schema; Hibernate validates it rather than updating it.
- Health probes, Prometheus metrics, graceful shutdown and instance/version information.
- Integration tests include rollback, invalid inputs, concurrent ordering and cancellation.

This is a learning backend. Authentication/authorization, payment, shipping, order
creation idempotency and production database operations are outside this starter.
Keep it local until access control has been added. In particular, retrying a successful
order creation with the same body creates another order. Cancellation is idempotent.

## API reference

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/info` | See app version and the current instance |
| POST | `/api/products` | Create `{sku,name,price,stock}` |
| GET | `/api/products?page=0&size=20` | List products |
| GET | `/api/products/{id}` | Read a product |
| POST | `/api/products/{id}/stock` | Add stock with `{quantity}` |
| POST | `/api/orders` | Place `{items:[{productId,quantity}]}` |
| GET | `/api/orders?page=0&size=20` | List orders |
| GET | `/api/orders/{id}` | Read an order |
| POST | `/api/orders/{id}/cancel` | Cancel and restore inventory once |
| GET | `/actuator/health/liveness` | Application liveness |
| GET | `/actuator/health/readiness` | Application readiness, including DB connectivity |
| GET | `/actuator/prometheus` | Prometheus metrics |

Pages are zero-based. Page size is between 1 and 100. An order has at most 20
different products; quantity is 1–10,000 per product. Prices must be positive,
with at most two decimal places. Fractional quantities are rejected.

## Switch to PostgreSQL

Create a PostgreSQL database and user named `stockflow`, or supply your own values.
The user must be able to create tables for Flyway migrations.
Use PostgreSQL 16 or 17 for this learning project.

```powershell
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/stockflow"
$env:DATABASE_USERNAME="stockflow"
$env:DATABASE_PASSWORD="your-local-password"
.\mvnw.cmd spring-boot:run
```

Do **not** enable the demo profile in this mode. The default username/password in
`application.yml` are local learning defaults; override them in containers or clusters.
The database host becomes the Compose/Kubernetes database **service name** when the
backend runs inside that environment. `localhost` inside a container refers to that
container, not to your laptop or the database container.

## Build an executable JAR

```powershell
.\mvnw.cmd clean verify
java -jar .\target\stockflow-1.0.0.jar --spring.profiles.active=demo
```

This JAR is the artifact we will package into a container.

## Tests against PostgreSQL

Tests use an isolated in-memory H2 database by default. To run the same test suite
against PostgreSQL, point it at a **dedicated disposable test database**:

```powershell
$env:TEST_DATABASE_URL="jdbc:postgresql://localhost:5432/stockflow_test"
$env:TEST_DATABASE_USERNAME="stockflow"
$env:TEST_DATABASE_PASSWORD="your-local-password"
.\mvnw.cmd test
```

Tests create persistent rows in that database. They never delete pre-existing rows,
but must still be run against a separate database, not production or your main app DB.
Remove these variables afterwards with `Remove-Item Env:TEST_DATABASE_URL` etc.
H2 is useful for fast checks; it does not prove PostgreSQL behaviour.

## Configuration for our deployment lessons

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `PORT` | `8080` | HTTP port |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/stockflow` | JDBC connection string |
| `DATABASE_USERNAME` | `stockflow` | DB login |
| `DATABASE_PASSWORD` | `stockflow` | DB password |
| `APP_VERSION` | `1.0.0` | Version shown by `/api/info` |
| `HOSTNAME` | `local` | Instance shown by `/api/info` |

Readiness checks the database because this application needs it to serve requests.
Liveness deliberately does not depend on database availability: restarting every
backend instance will not repair a database outage. Startup still needs a database
connection to apply migrations; this will be useful for dependency/startup lessons.

## Next steps

See `docs/LEARNING_PATH.md`. Dockerfiles, Compose and Kubernetes manifests are
intentionally exercises we will build and explain together; the backend is ready.

## References

- Spring Boot 3.5: https://docs.spring.io/spring-boot/3.5/
- Spring Data JPA locking: https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html
- Docker multi-stage builds: https://docs.docker.com/build/building/multi-stage/
- Kubernetes basics: https://kubernetes.io/docs/tutorials/kubernetes-basics/
