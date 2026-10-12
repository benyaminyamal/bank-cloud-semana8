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

FROM runtime AS config
COPY --from=build /src/config-server/target/config-server-*.jar /app/app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/app/app.jar"]

FROM runtime AS customers
COPY --from=build /src/customers-service/target/customers-service-*.jar /app/app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/app/app.jar"]

FROM runtime AS payments
COPY --from=build /src/payments-service/target/payments-service-*.jar /app/app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/app/app.jar"]

FROM runtime AS batch
COPY --from=build /src/batch-service/target/batch-service-*.jar /app/app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/app/app.jar"]

FROM runtime AS bff-web
COPY --from=build /src/bff-web/target/bff-web-*.jar /app/app.jar
EXPOSE 8091
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/app/app.jar"]

FROM runtime AS bff-mobile
COPY --from=build /src/bff-mobile/target/bff-mobile-*.jar /app/app.jar
EXPOSE 8092
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/app/app.jar"]

FROM runtime AS bff-atm
COPY --from=build /src/bff-atm/target/bff-atm-*.jar /app/app.jar
EXPOSE 8093
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/app/app.jar"]
