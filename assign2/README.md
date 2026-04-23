# DistributedChat

## Architecture

- **shared**: Common data models (User, Room, Message, Session)
- **server**: TCP server that accepts client connections
- **client**: TCP client that connects to the server

## Running

Build once:

```bash
cd src
./gradlew build
```

Start the server:

```bash
java -cp "server/build/libs/server.jar:shared/build/libs/shared.jar" pt.up.fe.t06g10.server.ChatServer 8888
```

Start the client:

```bash
java -cp "client/build/libs/client.jar:shared/build/libs/shared.jar" pt.up.fe.t06g10.client.Main localhost 8888
```
