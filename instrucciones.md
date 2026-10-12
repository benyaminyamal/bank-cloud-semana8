# Instrucciones para ejecutar y probar

Cualquier persona con Docker, Java 21 y Maven puede levantar el banco y comprobar cada componente. Los datos que se leen son los CSV de `data/semana_3`.

## Qué hay que tener instalado

- Docker con Docker Compose
- Java 21
- Maven
- `curl`

Desde la raíz del proyecto:

```bash
java -version
mvn -version
docker compose version
```

`java -version` tiene que decir 21. Si Maven usa otra versión, exporta `JAVA_HOME` al JDK 21 antes de correr los tests.

## 1. Levantar todo

En la raíz del repositorio:

```bash
docker compose up -d --build
```

La primera vez construye las imágenes y puede tardar varios minutos. Cuando termine:

```bash
docker compose ps
```

Todos los servicios tienen que quedar `healthy`. Si alguno sigue en `starting`, espera y vuelve a mirar. Eureka tarda un poco en registrar a los demás.

Si el puerto 8080 ya está ocupado en la máquina, el gateway no publica su puerto y `docker compose ps` lo muestra reiniciando. Hay que liberar el 8080 y volver a levantar el gateway.

Para apagar:

```bash
docker compose down
```

## 2. Pedir un token

Casi todas las APIs piden un JWT. El health queda público.

Token del núcleo (cuentas, transacciones, clientes, pagos, batch y eventos):

```bash
TOKEN=$(curl -s -u bank-client:bank-secret \
  -d grant_type=client_credentials \
  -d scope="accounts.read accounts.write transactions.read transactions.write events.read customers.read payments.read payments.write batch.run" \
  http://localhost:9000/oauth2/token | python3 -c 'import sys,json; print(json.load(sys.stdin)["access_token"])')
echo "$TOKEN"
```

Tiene que imprimir un texto largo. Dura una hora. Si una llamada responde 401, pide el token de nuevo.

Los canales usan otro cliente cada uno. Se piden más abajo, en la sección de cada BFF.

Si una llamada al gateway responde que el servicio no está disponible, Eureka todavía no lo registró. Espera unos segundos y repite. Cuando el servicio está arriba, la misma llamada devuelve los datos.

## 3. Kafka

Tres brokers (`kafka-1`, `kafka-2`, `kafka-3`). Cada topic nuevo sale con 3 particiones y replicación 3.

Prueba:

1. Abre http://localhost:8088
2. Entra al cluster `kafka-local`
3. Revisa los topics `bank.transacciones`, `bank.pagos` y `bank.alertas` después de probar transacciones, pagos y un retiro grande

En un topic nuevo se esperan 3 particiones y factor de replicación 3.

## 4. Discovery (Eureka)

Abre http://localhost:8761

En la lista de instancias tienen que aparecer, en estado UP:

- AUTH-SERVICE
- CONFIG-SERVER
- ACCOUNTS-SERVICE
- TRANSACTIONS-SERVICE
- NOTIFICATION-SERVICE
- CUSTOMERS-SERVICE
- PAYMENTS-SERVICE
- BATCH-SERVICE
- GATEWAY-SERVICE
- BFF-WEB
- BFF-MOBILE
- BFF-ATM

Health directo:

```bash
curl -s http://localhost:8761/actuator/health
```

La respuesta trae `"status":"UP"`.

## 5. Auth (OAuth2)

Puerto 9000. Emite el JWT del paso 2.

```bash
curl -s http://localhost:9000/actuator/health
curl -s -u bank-client:bank-secret \
  -d grant_type=client_credentials \
  -d scope="accounts.read" \
  http://localhost:9000/oauth2/token
```

El health responde UP. El token responde un JSON con `access_token` y `token_type` igual a `Bearer`.

Clientes que existen:

| Cliente | Secreto | Para qué |
| --- | --- | --- |
| bank-client | bank-secret | API del núcleo |
| web-client | web-secret | BFF web, scope `bff.web` |
| mobile-client | mobile-secret | BFF móvil, scope `bff.mobile` |
| atm-client | atm-secret | BFF cajero, scope `bff.atm` |

## 6. Config Server

Puerto 8888. Comparte logging y la exposición de actuator. No fija el puerto de los demás servicios.

```bash
curl -s http://localhost:8888/actuator/health
curl -s http://localhost:8888/application/default
```

El health responde UP. La segunda llamada devuelve la configuración compartida, con el patrón de log y `health,info,prometheus`.

## 7. Cuentas

Puerto 8081. Lee `intereses.csv` y `cuentas_anuales.csv`. También se llega por el gateway en el puerto 8080.

```bash
curl -s http://localhost:8081/actuator/health
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/cuentas/resumen
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/cuentas/106
```

En el resumen de la semana 3 se espera:

- `totalRegistros`: 1000
- `cuentasValidas`: 17
- `saldoTotal`: 162000
- `disponible`: true

La cuenta 106 es Jane Smith, saldo 12000, tipo prestamo.

Abrir, mantener y cerrar una cuenta:

```bash
curl -s -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"Ana Soto","saldo":5000,"edad":30,"tipo":"ahorro"}' \
  http://localhost:8080/api/cuentas

curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"Ana Soto","saldo":5000,"edad":30,"tipo":"prestamo"}' \
  http://localhost:8080/api/cuentas/EL_ID

curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/cuentas/EL_ID/cerrar
```

Cambia `EL_ID` por el id que devolvió el alta. El cierre responde la misma cuenta. Un id que no existe responde 404.

## 8. Transacciones

Puerto 8082. Lee `transacciones.csv` y publica en `bank.transacciones`.

```bash
curl -s http://localhost:8082/actuator/health
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/transacciones/informe
```

En el informe de la semana 3 se espera:

- `totalRegistros`: 1000
- `transaccionesValidas`: 401
- `montoTotal`: 527000
- `cuentasDisponibles`: true
- `mensaje`: Informe completo

Crear una transacción válida:

```bash
curl -s -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"fecha":"2024-06-30","monto":1500,"tipo":"credito"}' \
  http://localhost:8080/api/transacciones
```

Responde 201, la transacción creada y `publicada` en true. Después, en Kafka UI, el topic `bank.transacciones` tiene un mensaje nuevo. Una fecha imposible o un tipo distinto de `credito` o `debito` responde 400.

## 9. Notificaciones

Puerto 8083. Escucha `bank.transacciones`, `bank.pagos` y `bank.alertas`.

```bash
curl -s http://localhost:8083/actuator/health
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/eventos
```

Después de crear la transacción del paso anterior, la lista trae ese evento. Si todavía está vacía, espera un segundo y vuelve a llamar.

## 10. Clientes

Puerto 8084. Arma el perfil con las filas de `intereses.csv`.

```bash
curl -s http://localhost:8084/actuator/health
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/clientes
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/clientes/106
```

La lista no viene vacía. El cliente 106 responde su nombre y su tipo de cuenta.

## 11. Pagos

Puerto 8085. Guarda el pago y publica en `bank.pagos`. Un retiro de 50000 o más publica además una alerta en `bank.alertas`.

```bash
curl -s http://localhost:8085/actuator/health

curl -s -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"tipo":"deposito","cuentaOrigen":"106","monto":2000}' \
  http://localhost:8080/api/pagos

curl -s -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"tipo":"retiro","cuentaOrigen":"106","monto":50000}' \
  http://localhost:8080/api/pagos

curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/pagos
```

El depósito responde 201, `eventoPublicado` true y `alertaPublicada` false. El retiro de 50000 responde `alertaPublicada` true. El listado muestra los dos. En Kafka UI aparecen mensajes en `bank.pagos` y en `bank.alertas`.

Una transferencia sin cuenta destino responde 400:

```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"tipo":"transferencia","cuentaOrigen":"106","monto":1000}' \
  http://localhost:8080/api/pagos
```

Tiene que imprimir 400.

## 12. Batch

Puerto 8086. Los jobs no parten solos. Se lanzan por REST. Cada step lee, procesa y escribe de a 200 filas. Si la base del step falla, reintenta tres veces.

Lanza el reporte diario directo al servicio, porque lee las mil filas:

```bash
curl -s http://localhost:8086/actuator/health

curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8086/api/batch/jobs/reporteTransaccionesDiarias
```

En `reporte` se espera `leidas` 1000, `validas` 401 y `montoValidas` 527000. El mismo resultado queda en:

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8086/api/batch/reportes/reporteTransaccionesDiarias
```

Intereses y estado anual:

```bash
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8086/api/batch/jobs/calculoInteresesMensuales

curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8086/api/batch/jobs/estadoCuentaAnual
```

En intereses, `tasaAhorroMensual` es `0.5%` y `tasaPrestamoMensual` es `1.2%`. La hipoteca no entra. En el estado anual, `movimientosValidos` es mayor que cero.

Un nombre de job que no existe responde 404. El gateway también enruta `/api/batch/**` en el puerto 8080.

Para repetir la misma prueba sin Docker:

```bash
mvn -pl batch-service -am test -Dtest=ReporteDiarioJobTest
```

El test comprueba las mil filas, las 401 válidas y el monto 527000.

## 13. Gateway

Puerto 8080. Es la puerta de la API común. Resuelve el servicio por Eureka (`lb://...`) y, si el destino no responde, devuelve el fallback del circuit breaker.

```bash
curl -s http://localhost:8080/actuator/health
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/cuentas/resumen
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/transacciones/informe
```

Con los servicios arriba, el resumen y el informe son los del paso de cuentas y el de transacciones.

Para ver el fallback, detén cuentas y vuelve a pedir el informe:

```bash
docker compose stop accounts-service
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/transacciones/informe
docker compose start accounts-service
```

`cuentasDisponibles` queda en false y el mensaje dice que el informe es parcial. Las transacciones siguen respondiendo. Cuando cuentas vuelve a estar healthy, el informe completo regresa.

## 14. BFF web

Puerto 8091, HTTPS con certificado local. El navegador no entra por el gateway: llama a este BFF. El BFF pide su propio token interno y arma el panel con cuentas y clientes.

```bash
WEB=$(curl -s -u web-client:web-secret \
  -d grant_type=client_credentials \
  -d scope="bff.web" \
  http://localhost:9000/oauth2/token | python3 -c 'import sys,json; print(json.load(sys.stdin)["access_token"])')

curl -kfsS https://localhost:8091/actuator/health
curl -k -s -H "Authorization: Bearer $WEB" https://localhost:8091/api/panel
```

La respuesta trae `canal` igual a `web`, el resumen de cuentas (17 válidas y saldo 162000) y la lista de clientes. El token del núcleo (`bank-client`) no sirve aquí: este endpoint pide el scope `bff.web`.

## 15. BFF móvil

Puerto 8092, también HTTPS. Devuelve solo el saldo, no el panel.

```bash
MOBILE=$(curl -s -u mobile-client:mobile-secret \
  -d grant_type=client_credentials \
  -d scope="bff.mobile" \
  http://localhost:9000/oauth2/token | python3 -c 'import sys,json; print(json.load(sys.stdin)["access_token"])')

curl -kfsS https://localhost:8092/actuator/health
curl -k -s -H "Authorization: Bearer $MOBILE" https://localhost:8092/api/resumen
```

Se espera `canal` igual a `mobile`, `disponible` true, `cuentasValidas` 17 y `saldoTotal` 162000. No viene la lista de clientes.

## 16. BFF cajero

Puerto 8093, HTTPS. Consulta el saldo de una cuenta y manda el retiro a pagos.

```bash
ATM=$(curl -s -u atm-client:atm-secret \
  -d grant_type=client_credentials \
  -d scope="bff.atm" \
  http://localhost:9000/oauth2/token | python3 -c 'import sys,json; print(json.load(sys.stdin)["access_token"])')

curl -kfsS https://localhost:8093/actuator/health
curl -k -s -H "Authorization: Bearer $ATM" https://localhost:8093/api/saldo/106

curl -k -s -H "Authorization: Bearer $ATM" -H "Content-Type: application/json" \
  -d '{"cuentaId":"106","monto":1000}' \
  https://localhost:8093/api/retiros
```

El saldo responde `canal` igual a `cajero` y el saldo de la cuenta 106. El retiro responde `canal` igual a `cajero` y el pago en estado COMPLETADO. Un retiro de 50000 o más, con el mismo JSON, deja `alertaPublicada` en true.

## 17. Tests automáticos

Sin levantar Docker, desde la raíz:

```bash
mvn test
```

Tiene que terminar en `BUILD SUCCESS`. Cubre el parseo de los CSV, el token de auth y el job `reporteTransaccionesDiarias`.

## Resultado que tiene que cuadrar

Con la semana 3, cuentas y batch de transacciones tienen que decir lo mismo que el CSV ya filtrado:

- 1.000 cuentas leídas, 17 válidas, saldo 162.000
- 1.000 transacciones leídas, 401 válidas, monto 527.000
