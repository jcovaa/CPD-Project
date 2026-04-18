# DistributedChat

## Architecture

- **shared**: Common data models (User, Room, Message, Session)
- **server**: TCP server that accepts client connections
- **client**: TCP client that connects to the server

## Running

Start the server:

```bash
./gradlew server:run --args="8888"
```

Start the client:

```bash
./gradlew client:run --args="localhost 8888"
```
