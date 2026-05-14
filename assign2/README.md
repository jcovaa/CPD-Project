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

Start the Docker services:

```bash
docker compose up -d
```

Create the environment file:

```bash
cp server/.env.example server/.env
```

Start the server:

```bash
./gradlew :server:run --args="8888"
```

Start the client:

```bash
java -cp "client/build/libs/client.jar:shared/build/libs/shared.jar" pt.up.fe.t06g10.client.Main localhost 8888
```
