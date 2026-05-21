# DistributedChat

## Architecture

- `shared`: Common data models (User, Room, Message, Session)
- `server`: TLS-enabled TCP server that accepts client connections
- `client`: TLS-enabled TCP client that connects to the server

## Running

### Quickstart (Makefile)

If you have `make`, you can run everything with shorter commands:

```bash
cd src
make tls
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

Manual fallback instructions are in `RUN_MANUAL.md`.

### What this does (TLS setup)

The `make tls` target creates these files in `certs/`:

- `certs/server.p12` (server keystore with private key)
- `certs/server.crt` (server certificate exported from the keystore)
- `certs/client-truststore.p12` (client truststore)

If you already created these, `make tls` will keep them as-is.
