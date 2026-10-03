FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app

FROM runtime AS discovery
COPY --from=build /src/discovery-service/target/discovery-service-*.jar /app/app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/app/app.jar"]

FROM runtime AS auth
COPY --from=build /src/auth-service/target/auth-service-*.jar /app/app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/app/app.jar"]

FROM runtime AS accounts
COPY --from=build /src/accounts-service/target/accounts-service-*.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/app/app.jar"]

FROM runtime AS transactions
COPY --from=build /src/transactions-service/target/transactions-service-*.jar /app/app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/app/app.jar"]

FROM runtime AS notification
COPY --from=build /src/notification-service/target/notification-service-*.jar /app/app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/app/app.jar"]

FROM runtime AS gateway
COPY --from=build /src/gateway-service/target/gateway-service-*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/app/app.jar"]
