# Manual Build and Run (Fallback)

Use this if you do not want to use the `Makefile` targets.

## Prerequisites

- Java 21+
- Docker (for the PostgreSQL database)
- Gradle wrapper (included as `gradlew`)

## Database setup (one-time)

Start PostgreSQL and Ollama:

```bash
docker compose up -d
```

Create the environment file:

```bash
cp .env.example .env
```

## TLS setup (one-time)

Skip this section if you already have these files:

- `certs/server.p12` (server keystore with private key)
- `certs/client-truststore.p12` (client truststore)

You can either run `make tls` or follow the manual steps below.

```bash
cd src

# Create server keystore with a self-signed cert (change the CN if needed)
keytool -genkeypair \
  -alias server \
  -keyalg RSA \
  -keysize 2048 \
  -validity 365 \
  -keystore certs/server.p12 \
  -storetype PKCS12 \
  -storepass changeit \
  -keypass changeit \
  -dname "CN=localhost"

# Export the server certificate
keytool -exportcert \
  -alias server \
  -keystore certs/server.p12 \
  -storetype PKCS12 \
  -storepass changeit \
  -rfc \
  -file certs/server.crt

# Create client truststore and import the server certificate
keytool -importcert \
  -alias server \
  -file certs/server.crt \
  -keystore certs/client-truststore.p12 \
  -storetype PKCS12 \
  -storepass changeit \
  -noprompt
```

## Build

```bash
cd src
./gradlew build
```

This produces fat JARs (all dependencies bundled) at:

- `server/build/libs/server.jar`
- `client/build/libs/client.jar`

## Start the server (TLS)

```bash
cd src
java -Djavax.net.ssl.keyStore=certs/server.p12 \
     -Djavax.net.ssl.keyStorePassword=changeit \
     -Djavax.net.ssl.keyStoreType=PKCS12 \
     -cp "server/build/libs/server.jar" \
     pt.up.fe.t06g10.server.Main 8888
```

## Start the client (TLS)

```bash
cd src
java -Djavax.net.ssl.trustStore=certs/client-truststore.p12 \
     -Djavax.net.ssl.trustStorePassword=changeit \
     -Djavax.net.ssl.trustStoreType=PKCS12 \
     -cp "client/build/libs/client.jar" \
     pt.up.fe.t06g10.client.Main localhost 8888
```

## Avoid repeating JVM flags (optional)

You can export JVM options per terminal so you do not retype them:

```bash
# terminal 1 (server)
export JAVA_TOOL_OPTIONS="-Djavax.net.ssl.keyStore=certs/server.p12 -Djavax.net.ssl.keyStorePassword=changeit -Djavax.net.ssl.keyStoreType=PKCS12"

# terminal 2 (client)
export JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStore=certs/client-truststore.p12 -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStoreType=PKCS12"
```

Then run the same `java -cp ...` commands without the `-D` flags.

## Check postgres database

Use psql inside the container:

```bash
docker exec -it <container-name> psql -U <user> -d chatdb
```

See tables:

```bash
\dt
```
