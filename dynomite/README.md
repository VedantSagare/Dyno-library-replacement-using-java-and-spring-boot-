# Dynomite

Dynomite is a Spring Boot project that simulates a distributed key/value proxy with consistent hashing and best-effort replication.

## What It Does

- routes keys to nodes using a consistent-hash ring
- replicates writes across peer nodes
- exposes a cluster-facing API at `/v1/kv`
- exposes an internal node-local API at `/v1/internal/kv`
- stores data in memory with optional TTL

## Prerequisites

- Java 21

## Run the Application

From the `dynomite` directory:

```powershell
.\mvnw.cmd spring-boot:run
```

This starts:

- profile: `node1`
- port: `9091`

## Run a 3-Node Local Cluster

From the repository root, use 3 separate terminals:

```powershell
cd .\dynomite
.\mvnw.cmd spring-boot:run
```

```powershell
cd .\dynomite
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=node2"
```

```powershell
cd .\dynomite
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=node3"
```

Default ports:

- `node1` -> `9091`
- `node2` -> `9092`
- `node3` -> `9093`

## Run Through an IDE

Run the main class:

- `com.crowdfunding.dynomite.DynomiteApplication`

To start other nodes, pass one of these program arguments:

```text
--spring.profiles.active=node2
```

```text
--spring.profiles.active=node3
```

## API Examples

### Put a Value

```powershell
Invoke-RestMethod -Method Put -Uri http://127.0.0.1:9091/v1/kv/foo -ContentType application/json -Body '{"value":"bar"}'
```

### Get a Value

```powershell
Invoke-RestMethod -Method Get -Uri http://127.0.0.1:9092/v1/kv/foo
```

### Delete a Value

```powershell
Invoke-RestMethod -Method Delete -Uri http://127.0.0.1:9093/v1/kv/foo
```

### Example Request Body

```json
{
  "value": "bar",
  "ttlSeconds": 30
}
```

## Configuration

Cluster and node settings are defined in:

- `src/main/resources/application.yml`

Important properties:

- `dynomite.node-id`
- `dynomite.nodes`
- `dynomite.replication-factor`
- `dynomite.virtual-nodes`
- `dynomite.request-timeout-millis`

## Test

```powershell
.\mvnw.cmd test
```

## Current Limitations

- in-memory storage only
- no persistence
- best-effort replication
- intended for learning, experimentation, and extension
