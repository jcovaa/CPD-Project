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

Set AI configuration in `.env`:

```
AI_OLLAMA_URL=http://localhost:11434/api/chat
AI_MODEL=llama3
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

## commands

The client has several commands to interact with the server. 

As a client, you can run `HELP` to see the list of commands available.

```
> HELP
```

Here is the list of commands available:

1. Authentication commands
    - REGISTER \<username> \<password> - Create a new user account
    - AUTH \<username> \<password> - Login with username and password
    - TOKEN \<token> - Authenticate using a session token
    - RECONECT \<token> - Reconnect an existing session 
    - LOGOUT - Log out of the current session

2. Room commands (Needs to be Authenticated)
    - LIST_ROOMS - List available chat rooms
    - CREATE_ROOM \<roomName> \[prompt] - Create a new room. If prompt given, the room creates a bot with the specific prompt as context
    - JOIN_ROOM \<roomName> - Join the specified room
    - LEAVE_ROOM - Leave the current room
    - HISTORY \<roomName> \[count] - Show recent messages from a room

3. Message commands (Needs to be inside a room)
    - SEND \<message> - Send a message to the current room
    - SEND_AI \<message> - Send a message that prompts the bot to answer

4. General commands
    - HELP - Show the help text/menu
    - QUIT - Disconnect

## Authors

| Name | Number |
| ---- | ------ |
| Constança Lemos Ferreira | 202306850 |
| João Rodrigues Vila Cova | 202307756 |
| Pedro Andrade de Castro | 202200044 |
