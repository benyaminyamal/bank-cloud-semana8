# Banco XYZ — microservicios y resiliencia en la nube

Actividad sumativa: desarrollar microservicios y resiliencia en la nube con Spring Cloud, tomando como datos base el legado de [bank_legacy_data](https://github.com/KariVillagran/bank_legacy_data).

## Objetivo

Exponer la información histórica del Banco XYZ (cuentas, intereses y transacciones) mediante microservicios independientes, protegidos con OAuth 2.0, tolerantes a fallos y comunicados de forma asíncrona. El sistema se levanta completo con Docker Compose.

## Propuesta técnica

La solución separa el acceso, la seguridad y el negocio:

- **discovery-service** registra los microservicios con Eureka para que el gateway los encuentre por nombre.
- **auth-service** es el servidor de autorización. Emite JWT con el grant `client_credentials`.
- **gateway-service** es la única entrada HTTP. Rechaza peticiones sin token y aplica un circuit breaker por ruta.
- **accounts-service** carga `intereses.csv` y `cuentas_anuales.csv`. Cada fila queda marcada como válida o con sus observaciones (saldo vacío, edad fuera de rango, tipo inválido, duplicados). También abre, cierra y mantiene cuentas.
- **customers-service** publica el perfil de cada cliente a partir del mismo legado.
- **payments-service** registra depósitos, retiros y transferencias, y publica el pago en Kafka. Un retiro de 50.000 o más genera además una alerta de seguridad.
- **transactions-service** carga `transacciones.csv`, publica eventos en Kafka y consulta el resumen de cuentas. Si cuentas no responde, Resilience4j devuelve el informe solo con las transacciones.
- **notification-service** consume `bank.transacciones`, `bank.pagos` y `bank.alertas`.
- **batch-service** reescribe los tres procesos del legado: reporte diario de anomalías, interés mensual de ahorro y préstamo, y estado de cuenta anual. Cada job lee, procesa y escribe en chunks de 200, reintenta fallos transitorios de base de datos y se puede relanzar si queda en fallo.
- **config-server** entrega la configuración compartida (registro y formato de log) a los demás servicios.
- **bff-web**, **bff-mobile** y **bff-atm** son tres backends de canal, independientes entre sí y del gateway. Hablan HTTPS con certificado propio y piden al núcleo solo lo que su canal necesita.
- **Kafka** corre con 3 brokers. Los topics de negocio tienen 3 particiones y replicación 3.

Los CSV de `data/semana_1`, `data/semana_2` y `data/semana_3` se copian dentro de las imágenes. El perfil por defecto es `semana_3`.

El cliente de demostración es `bank-client` / `bank-secret`. Cubre lectura y escritura de cuentas, transacciones, clientes, pagos y la ejecución de batch. `transactions-internal` lee cuentas desde transacciones. `bff-internal` lo hace desde los canales. `web-client`, `mobile-client` y `atm-client` solo hablan con su BFF. Esas claves son de la entrega, no de un ambiente real.

## Estructura

```
bank-cloud-semana8/
├── pom.xml                      # proyecto Maven multi-módulo
├── Dockerfile                   # una etapa de imagen por microservicio
├── docker-compose.yml           # Eureka, OAuth2, Kafka y los servicios
├── data/                        # CSV legacy por semana
├── common/                      # lectura de CSV, fechas y el evento de Kafka
├── discovery-service/           # Eureka, puerto 8761
├── auth-service/                # OAuth 2.0, puerto 9000
├── accounts-service/            # cuentas, puerto 8081
├── transactions-service/        # transacciones, Resilience4j y productor Kafka, puerto 8082
├── notification-service/        # consumidor Kafka, puerto 8083
├── customers-service/           # perfiles de cliente, puerto 8084
├── payments-service/            # pagos y alertas, puerto 8085
├── batch-service/               # Spring Batch, puerto 8086
├── config-server/               # Spring Cloud Config, puerto 8888
├── bff-web/                     # canal navegador, HTTPS 8091
├── bff-mobile/                  # canal móvil, HTTPS 8092
├── bff-atm/                     # canal cajero, HTTPS 8093
└── gateway-service/             # entrada y circuit breaker, puerto 8080
```

Stack: Java 21, Spring Boot 3.5, Spring Cloud 2025.0, Resilience4j, Kafka y Docker.

## Cómo ejecutarlo

Requisitos: JDK 21 o superior, Maven 3.9 y Docker.

```bash
docker compose up --build
```

Para compilar y probar sin contenedores:

```bash
mvn test
```

### Token manual

```bash
curl -u bank-client:bank-secret \
  -d grant_type=client_credentials \
  -d scope="accounts.read accounts.write transactions.read transactions.write events.read customers.read payments.read payments.write batch.run" \
  http://localhost:9000/oauth2/token
```

### Rutas

Todas pasan por el gateway (`http://localhost:8080`) y piden `Authorization: Bearer <token>`.

| Método | Ruta | Scope |
| --- | --- | --- |
| GET | `/api/cuentas` y `/api/cuentas?soloValidas=true` | `accounts.read` |
| GET | `/api/cuentas/resumen` | `accounts.read` |
| GET | `/api/cuentas/{id}` y `/api/cuentas/{id}/anual` | `accounts.read` |
| GET | `/api/transacciones` y `/api/transacciones/{id}` | `transactions.read` |
| GET | `/api/transacciones/informe` | `transactions.read` |
| POST | `/api/transacciones` | `transactions.write` |
| GET | `/api/eventos` | `events.read` |
| POST | `/api/cuentas` | `accounts.write` |
| POST | `/api/cuentas/{id}/cerrar` y PUT `/api/cuentas/{id}` | `accounts.write` |
| GET | `/api/clientes` y `/api/clientes/{id}` | `customers.read` |
| GET y POST | `/api/pagos` | `payments.read` / `payments.write` |
| POST | `/api/batch/jobs/{nombre}` | `batch.run` |

Ejemplo de alta:

```bash
curl -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"fecha":"2024-12-02","monto":2200,"tipo":"debito"}' \
  http://localhost:8080/api/transacciones
```

Para ver el corte de Resilience4j, con el sistema arriba:

```bash
docker compose stop accounts-service
```

`GET /api/cuentas` responde el fallback del gateway. `GET /api/transacciones/informe` sigue entregando las transacciones con `cuentasDisponibles` en falso. Después se vuelve a levantar cuentas con `docker compose start accounts-service`.

## Procesos batch

Los tres jobs se lanzan a demanda. Los nombres son `reporteTransaccionesDiarias`, `calculoInteresesMensuales` y `estadoCuentaAnual`.

```bash
curl -H "Authorization: Bearer $TOKEN" -X POST \
  http://localhost:8080/api/batch/jobs/reporteTransaccionesDiarias
```

El reporte diario clasifica las mismas anomalías que el parser de transacciones (id, fecha, monto, tipo y duplicados) y totaliza solo las filas válidas. El interés mensual aplica 0,5 % a las cuentas de ahorro válidas y 1,2 % a los préstamos válidos. El estado anual agrupa depósitos y retiros válidos por cuenta. Si una ejecución queda en fallo, `POST /api/batch/ejecuciones/{id}/reiniciar` la retoma; el servicio también revisa cada 20 segundos y reejecuta una vez los fallos críticos. Un corte transitorio de la base del `JobRepository` se reintenta tres veces dentro del step.

## Canales BFF

Cada canal tiene su cliente OAuth y su propio servicio. No pasan por el gateway: el certificado es local, así que `curl` usa `-k`.

| Canal | Puerto | Cliente | Qué entrega |
| --- | --- | --- | --- |
| Web | 8091 | `web-client` / `web-secret`, scope `bff.web` | panel completo de cuentas y clientes |
| Móvil | 8092 | `mobile-client` / `mobile-secret`, scope `bff.mobile` | cuentas válidas y saldo total |
| Cajero | 8093 | `atm-client` / `atm-secret`, scope `bff.atm` | saldo de una cuenta y retiro |

```bash
curl -k -u web-client:web-secret -d grant_type=client_credentials -d scope=bff.web \
  http://localhost:9000/oauth2/token
curl -k -H "Authorization: Bearer $TOKEN_WEB" https://localhost:8091/api/panel
```

Si cuentas o pagos no responden, el BFF devuelve la respuesta alternativa de Resilience4j en lugar de propagar el corte.

## Despliegue

Las imágenes del `Dockerfile` son el artefacto para un entorno como AWS. La configuración vive en Config Server y en variables de entorno, no dentro del código. Cada servicio expone `/actuator/health` para un balanceador, Eureka permite sumar instancias del mismo servicio, y Kafka queda con factor de replicación 3. `docker compose up --build` levanta esa topología en local.

## Evidencia de ejecución

`docker compose ps` dejó los siete componentes en estado healthy: `discovery-service`, `auth-service`, `accounts-service`, `transactions-service`, `notification-service`, `gateway-service` y `kafka`.


Arranque de cada microservicio:

```
Tomcat started on port 8761
Started DiscoveryServiceApplication

Tomcat started on port 9000
Started AuthServiceApplication

Tomcat started on port 8081
Started AccountsServiceApplication

Tomcat started on port 8082
Started TransactionsServiceApplication

Tomcat started on port 8083
Started NotificationServiceApplication

Netty started on port 8080
Started GatewayServiceApplication
```

Servicios registrados en Eureka: `ACCOUNTS-SERVICE`, `AUTH-SERVICE`, `GATEWAY-SERVICE`, `NOTIFICATION-SERVICE`, `TRANSACTIONS-SERVICE`.

Salud de cada uno (`/actuator/health`): `{"status":"UP"}`.

Token OAuth 2.0 (`POST /oauth2/token`):

```
token_type: Bearer
expires_in: 3599
scope: accounts.read transactions.read transactions.write events.read
```

Sin token, el gateway responde `401`.

Resumen de cuentas (`GET /api/cuentas/resumen`):

```json
{"disponible":true,"totalRegistros":1000,"cuentasValidas":17,"saldoTotal":162000,"mensaje":"ok"}
```

Informe que cruza transacciones y cuentas (`GET /api/transacciones/informe`):

```json
{"totalRegistros":1000,"transaccionesValidas":401,"montoTotal":527000,"cuentasDisponibles":true,"cuentas":{"disponible":true,"totalRegistros":1000,"cuentasValidas":17,"saldoTotal":162000,"mensaje":"ok"},"mensaje":"Informe completo"}
```

Alta publicada en Kafka (`POST /api/transacciones`, HTTP 201):

```json
{"transaccion":{"id":1001,"fechaOriginal":"2024-12-02","fecha":"2024-12-02","monto":2200,"tipo":"debito","valida":true,"observaciones":[]},"eventoPublicado":true}
```

Eventos consumidos (`GET /api/eventos`):

```json
[
  {"tipoEvento":"TRANSACCION_CREADA","transaccionId":1001,"tipoMovimiento":"debito","monto":2200,"fecha":"2024-12-02","detalle":"Transacción registrada"},
  {"tipoEvento":"IMPORTACION","monto":527000,"detalle":"Importación legacy: 1000 transacciones"}
]
```
