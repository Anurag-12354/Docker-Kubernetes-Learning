# Docker and Kubernetes practice with StockFlow

Work through one checkpoint at a time. For each step, predict the behaviour, run
the experiment, inspect the result, then record why it happened.

## 1. Package one application

- Explain image vs container vs Dockerfile vs running process.
- Build the JAR and a first Docker image; run the demo profile.
- Publish a host port; inspect logs and environment variables.
- Stop and remove the container; explain why in-memory data disappears.
- Add `.dockerignore`, caching and a multi-stage build; run as a non-root user.
- Compare image sizes before/after with recorded measurements.

Checkpoint: rebuild and run the backend from a clean folder without an IDE.

## 2. Add PostgreSQL with Docker Compose

- Start PostgreSQL and the backend as separate services.
- Connect by service name, not by an IP copied from a running container.
- Add a named database volume and a database health check.
- Recreate containers and verify that product/order data remains.
- Break the DB hostname and password separately; investigate each failure.
- Distinguish image storage, container writable layers, bind mounts and volumes.

Checkpoint: explain what survives `docker compose down`, and what `down -v` removes.

## 3. Move to Minikube

- Explain the control plane, nodes, kubelet, container runtime and scheduler.
- Inspect the active context and create a dedicated namespace.
- Load a tagged application image into Minikube or use an accessible registry.
- Write a backend Deployment and a ClusterIP Service.
- Add PostgreSQL using a single-replica StatefulSet, Service and PVC for the lab.
- Put settings in a ConfigMap and credentials in a Secret.
- Access the app with port-forwarding first; then study external access/routing.

Checkpoint: explain each manifest and trace a request from your laptop to the DB.
Kubernetes Secrets are not automatically an encrypted vault. Base64 encoding alone
does not provide confidentiality. Keep real credentials out of Git.

## 4. Operate and deliberately break the deployment

- Configure startup, readiness and liveness probes using Actuator.
- Set resource requests/limits; observe scheduling and container memory behaviour.
- Scale the backend to three replicas, keeping the DB at one replica in this lab.
- Use `/api/info` to inspect instance names and versions.
- Delete a backend Pod and observe its replacement.
- Stop the database and inspect readiness, events, logs and connection errors.
- Replace the database Pod and verify data survives; understand the limits of local PVCs.
- Publish a new immutable image tag, perform a rolling update, and practise rollback.
- Diagnose an incorrect image tag, wrong Service selector, and failing health probe.

Checkpoint: diagnose a failure with `get`, `describe`, `logs`, events and `exec`.
Three replicas on one Minikube node do not provide node-level high availability.
A single PostgreSQL StatefulSet is a storage exercise, not a production HA database.

## 5. Delivery, monitoring and supporting skills

- Build and test on a GitHub Actions runner; publish tagged images to a registry.
- Distinguish CI (automated checks/builds) from CD (delivery/deployment).
- Add deployment automation only when the runner has a deliberate, secure route
  to the target cluster. A hosted runner cannot reach your laptop's Minikube by default.
- Add Prometheus/Grafana and inspect request rate, errors, latency and JVM metrics.
- Practise Linux processes, permissions, DNS, TCP ports and HTTP while debugging.
- Later: Helm packaging, service accounts/RBAC, and HPA with a metrics provider.
- Evaluate cloud deployment as a separate step after understanding local operation.

## Portfolio evidence

- README with reproducible commands and the deployed architecture.
- Tests and a successful CI run.
- Recorded image sizes, rollout behaviour and a concurrent-stock experiment.
- A short incident log: symptom, investigation, cause, fix, prevention.
- Clearly identify the supplied backend code and the deployment/operations work you
  completed yourself. Describe only capabilities you actually implemented and tested.
