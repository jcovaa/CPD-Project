FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

COPY . .

RUN gradle :server:jar :shared:jar --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/server/build/libs/server.jar ./server.jar
COPY --from=builder /app/shared/build/libs/shared.jar ./shared.jar
COPY --from=builder /app/server/users.txt ./users.txt

EXPOSE 8888

ENTRYPOINT ["java", "-cp", "server.jar:shared.jar", "pt.up.fe.t06g10.server.Main", "8888"]