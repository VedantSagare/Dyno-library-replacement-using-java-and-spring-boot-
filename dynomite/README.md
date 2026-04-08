# Dynomite (Learning Scaffold)

This folder contains a small Dynomite-like key/value proxy built with Java 21 + Spring Boot.

What it does:
- Consistent-hash ring with virtual nodes
- Replication by forwarding writes to replica nodes (best-effort)
- External API (`/v1/kv/*`) that routes via the ring
- Internal API (`/v1/internal/kv/*`) that applies a request locally (used for replication)

## Run a 3-node local cluster

This repository already has a Maven wrapper in `backend/`. You can reuse it to run `dynomite/`.

In 3 separate terminals (from repo root):

```powershell
.\backend\mvnw.cmd -f .\dynomite\pom.xml spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=node1"
.\backend\mvnw.cmd -f .\dynomite\pom.xml spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=node2"
.\backend\mvnw.cmd -f .\dynomite\pom.xml spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=node3"
```

## Try it

```powershell
Invoke-RestMethod -Method Put -Uri http://127.0.0.1:9091/v1/kv/foo -ContentType application/json -Body '{"value":"bar"}'
Invoke-RestMethod -Method Get -Uri http://127.0.0.1:9092/v1/kv/foo
Invoke-RestMethod -Method Delete -Uri http://127.0.0.1:9093/v1/kv/foo
```

## Notes
- This is not production Dynomite. It’s a scaffold to evolve into quorum reads/writes, gossip membership, hinted handoff, etc.

