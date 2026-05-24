# DistributedChat

## Architecture

- **shared**: Common data models (User, Room, Message, Session)
- **server**: TCP server that accepts client connections
- **client**: TCP client that connects to the server

## Running

Build once:

```bash
cd src
./gradlew shadowJar
```

Start the Docker services:

```bash
docker compose up -d
```

Create the environment file:

```bash
cp .env.example .env
```

Start the server:

```bash
java -jar server/build/libs/server.jar 8888
```

Start the client:

```bash
java -jar client/build/libs/client.jar localhost 8888
```
