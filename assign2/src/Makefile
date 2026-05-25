SHELL := /bin/bash

HOST ?= localhost
PORT ?= 8888
CN ?= localhost
STOREPASS ?= changeit
KEYPASS ?= changeit
JAVA ?= java

SERVER_JAR := server/build/libs/server.jar
CLIENT_JAR := client/build/libs/client.jar

.PHONY: build tls server client clean-certs

build:
	./gradlew build

tls:
	@mkdir -p certs
	@if [ -f certs/server.p12 ] && [ -f certs/client-truststore.p12 ]; then \
		echo "TLS files already exist in certs/"; \
	else \
		keytool -genkeypair \
		  -alias server \
		  -keyalg RSA \
		  -keysize 2048 \
		  -validity 365 \
		  -keystore certs/server.p12 \
		  -storetype PKCS12 \
		  -storepass "$(STOREPASS)" \
		  -keypass "$(KEYPASS)" \
		  -dname "CN=$(CN)"; \
		keytool -exportcert \
		  -alias server \
		  -keystore certs/server.p12 \
		  -storetype PKCS12 \
		  -storepass "$(STOREPASS)" \
		  -rfc \
		  -file certs/server.crt; \
		keytool -importcert \
		  -alias server \
		  -file certs/server.crt \
		  -keystore certs/client-truststore.p12 \
		  -storetype PKCS12 \
		  -storepass "$(STOREPASS)" \
		  -noprompt; \
	fi

server: build
	$(JAVA) -Djavax.net.ssl.keyStore=certs/server.p12 \
	     -Djavax.net.ssl.keyStorePassword=$(STOREPASS) \
	     -Djavax.net.ssl.keyStoreType=PKCS12 \
	     -cp "$(SERVER_JAR)" \
	     pt.up.fe.t06g10.server.Main $(PORT)

client: build
	$(JAVA) -Djavax.net.ssl.trustStore=certs/client-truststore.p12 \
	     -Djavax.net.ssl.trustStorePassword=$(STOREPASS) \
	     -Djavax.net.ssl.trustStoreType=PKCS12 \
	     -cp "$(CLIENT_JAR)" \
	     pt.up.fe.t06g10.client.Main $(HOST) $(PORT)

clean-certs:
	rm -f certs/server.p12 certs/server.crt certs/client-truststore.p12
