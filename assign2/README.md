# DistributedChat

## Architecture

- **shared**: Common data models (User, Room, Message, Session)
- **server**: TLS-enabled TCP server that accepts client connections
- **client**: TLS-enabled TCP client that connects to the server

## Running

Build once:

```bash
cd src
./gradlew build
```

### TLS setup (one-time)

If you already created these, skip this section. Expected files:

- `certs/server.p12` (server keystore with private key)
- `certs/client-truststore.p12` (client truststore)

### Start the server (TLS)

```bash
cd src
java -Djavax.net.ssl.keyStore=certs/server.p12 \
     -Djavax.net.ssl.keyStorePassword=changeit \
     -Djavax.net.ssl.keyStoreType=PKCS12 \
     -cp "server/build/libs/server.jar:shared/build/libs/shared.jar" \
     pt.up.fe.t06g10.server.Main 8888
```

### Start the client (TLS)

```bash
cd src
java -Djavax.net.ssl.trustStore=certs/client-truststore.p12 \
     -Djavax.net.ssl.trustStorePassword=changeit \
     -Djavax.net.ssl.trustStoreType=PKCS12 \
     -cp "client/build/libs/client.jar:shared/build/libs/shared.jar" \
     pt.up.fe.t06g10.client.Main localhost 8888
```

### Avoid repeating JVM flags (optional)

You can export JVM options per terminal so you do not retype them:

```bash
# terminal 1 (server)
export JAVA_TOOL_OPTIONS="-Djavax.net.ssl.keyStore=certs/server.p12 -Djavax.net.ssl.keyStorePassword=changeit -Djavax.net.ssl.keyStoreType=PKCS12"

# terminal 2 (client)
export JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStore=certs/client-truststore.p12 -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStoreType=PKCS12"
```

Then run the same `java -cp ...` commands without the `-D` flags.

