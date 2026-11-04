# Verification

Checked on 3 November 2026 with OpenJDK 17 on Linux.

- Maven `verify`: successful; application compiled and executable JAR packaged.
- Integration suite: **12 tests, 0 failures, 0 errors, 0 skipped**, using H2 in PostgreSQL compatibility mode.
- Tests cover product creation, duplicate SKU handling, input validation, pagination,
  missing resources, stock replenishment, successful orders, insufficient stock,
  transaction rollback, large monetary values, concurrent orders and concurrent cancellation.
- Started the packaged JAR with the demo profile and checked it over real HTTP:
  health, deployment information, create product, create order, stock decrement,
  cancellation, stock restoration, readiness, liveness and Prometheus metrics.

## Still to verify during the Docker lessons

- The migration and JDBC configuration are supplied for PostgreSQL, but a live
  PostgreSQL instance could not be started in the build environment. The same test
  suite can target PostgreSQL using the variables documented in the README.
- Docker images, Compose configuration and Kubernetes manifests will be created
  and tested as the learning exercises. They are not included as completed work.
- PowerShell instructions and the standard Maven Windows wrapper are included;
  execution was checked on Linux, not on Windows.

H2 compatibility mode is not a substitute for PostgreSQL integration testing.
