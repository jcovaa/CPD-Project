# DistributedChat

## Architecture

- `server`: TLS-enabled TCP server that accepts client connections
- `client`: TLS-enabled TCP client that connects to the server

## Running

### Prerequisites

- Java 21+
- Docker (for PostgreSQL and Ollama)
- `make` (optional, simplifies commands)

### 1. Database and Ollama

Start PostgreSQL:

```bash
cd src
docker compose --profile cpu up -d
# Profile can be `nvidia` or `amd`
```

Create the environment file:

```bash
cp .env.example .env
```

Pull the model (only one time):
```bash
docker exec -it ollama ollama pull llama3
```

Check if Ollama up:
```
curl http://localhost:11434/api/tags
```

Or with docker:
```
docker ps | grep ollama
```

### 3. TLS setup (one-time)

```bash
cd src
make tls
```

This creates `certs/server.p12`, `certs/server.crt`, and `certs/client-truststore.p12`.

### 4. Quickstart (Makefile)

```bash
cd src
make server
```

In another terminal:

```bash
cd src
make client
```

Optional overrides:

```bash
# Use a different host/port or certificate CN
make HOST=127.0.0.1 PORT=9999 CN=127.0.0.1 tls

# Use a different keystore password
make STOREPASS=secret KEYPASS=secret tls
```

Manual fallback instructions are in [RUN_MANUAL.md](src/RUN_MANUAL.md).

### What this does (TLS setup)

The `make tls` target creates these files in `certs/`:

- `certs/server.p12` (server keystore with private key)
- `certs/server.crt` (server certificate exported from the keystore)
- `certs/client-truststore.p12` (client truststore)

If you already created these, `make tls` will keep them as-is.
